package com.yervant.huntgames.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.yervant.huntgames.R
import com.yervant.huntgames.ui.menu.AddressTableMenu
import com.yervant.huntgames.ui.menu.InitialMemoryMenu
import com.yervant.huntgames.ui.menu.ProcessMenu
import com.yervant.huntgames.ui.menu.SettingsMenu
import com.yervant.huntgames.ui.theme.HuntGamesTheme

class CustomOverlayView(context: android.content.Context) : View(context) {
    private var onClickCallback: (() -> Unit)? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val CLICK_DRAG_TOLERANCE = 10f
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var windowManager: WindowManager
    private var lastClickTime = 0L
    private val DOUBLE_CLICK_TIME_DELTA = 300L

    fun setWindowManager(wm: WindowManager, layoutParams: WindowManager.LayoutParams) {
        windowManager = wm
        params = layoutParams
    }

    fun setOnClickListener(callback: () -> Unit) {
        onClickCallback = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (abs(deltaX) > CLICK_DRAG_TOLERANCE || abs(deltaY) > CLICK_DRAG_TOLERANCE) {
                    isDragging = true
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    try {
                        (parent as? View)?.let { parentView ->
                            windowManager.updateViewLayout(parentView, params)
                        }
                    } catch (e: IllegalArgumentException) {
                        // Handle exception if view is not attached
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val clickTime = System.currentTimeMillis()
                    if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                        // Double click detected
                        lastClickTime = 0L
                    } else {
                        lastClickTime = clickTime
                        performClick()
                    }
                }
                (context as? OverlayService)?.updateLastKnownPosition(params.x, params.y)
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        onClickCallback?.invoke()
        return true
    }
}


class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner, DialogCallback {
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private lateinit var menuView: ComposeView
    private lateinit var touchInterceptor: CustomOverlayView
    private var isMenuVisible = false
    private val NOTIFICATION_CHANNEL_ID = "overlay_channel"
    private val NOTIFICATION_ID = 1
    var isDialogVisible = false
    private var selectedTabIndex = 0
    lateinit var dialogView: ComposeView

    var lastKnownX: Int = 0
    var lastKnownY: Int = 100

    fun updateLastKnownPosition(x: Int, y: Int) {
        lastKnownX = x
        lastKnownY = y
    }

    private val orientationEventListener by lazy {
        object : android.view.OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                updateViewsOnRotation()
            }
        }
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = lastKnownX
        y = lastKnownY
    }

    private val menuParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    private fun updateViewsOnRotation() {
        // Salva a posição atual
        lastKnownX = params.x
        lastKnownY = params.y

        if (::overlayView.isInitialized) {
            (overlayView.parent as? android.view.ViewGroup)?.let { parent ->
                try {
                    updateLayoutForCurrentOrientation()
                    windowManager.updateViewLayout(parent, params)
                } catch (e: IllegalArgumentException) {
                    // Handle exception
                }
            }
        }

        if (::menuView.isInitialized && isMenuVisible) {
            try {
                updateMenuLayoutForCurrentOrientation()
                windowManager.updateViewLayout(menuView, menuParams)
            } catch (e: IllegalArgumentException) {
                // Handle exception
            }
        }
    }

    private fun updateLayoutForCurrentOrientation() {
        val metrics = resources.displayMetrics
        val density = metrics.density
        val buttonSize = (48 * density).toInt()

        // Ajusta o tamanho do botão
        params.width = buttonSize
        params.height = buttonSize

        // Garante que o botão não saia da tela
        if (params.x > metrics.widthPixels - buttonSize) {
            params.x = metrics.widthPixels - buttonSize
        }
        if (params.y > metrics.heightPixels - buttonSize) {
            params.y = metrics.heightPixels - buttonSize
        }
        if (params.x < 0) params.x = 0
        if (params.y < 0) params.y = 0
    }

    private fun updateMenuLayoutForCurrentOrientation() {
        val metrics = resources.displayMetrics
        menuParams.width = metrics.widthPixels
        menuParams.height = metrics.heightPixels
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val containerLayout = android.widget.FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }

        overlayView = ComposeView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon),
                        contentDescription = "Floating Button",
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                    )
                }
            }
        }

        touchInterceptor = CustomOverlayView(this).apply {
            setWindowManager(windowManager, params)
            setOnClickListener { showMenu() }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        menuView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)

            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setViewTreeViewModelStoreOwner(this@OverlayService)

            setContent {
                HuntGamesTheme(darkTheme = true) {
                    MenuContent(
                        onClose = { hideMenu() }
                    )
                }
            }
        }

        containerLayout.addView(overlayView)
        containerLayout.addView(touchInterceptor)

        windowManager.addView(containerLayout, params)
        updateLayoutForCurrentOrientation()

        orientationEventListener.enable()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for overlay service"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

        return builder
            .setContentTitle("Overlay Service")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    @Composable
    private fun MenuContent(onClose: () -> Unit) {
        var selectedTab by remember { mutableIntStateOf(selectedTabIndex) }
        val tabs = listOf("Processes", "Memory", "Editor", "Settings")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    selectedTab = index
                                    selectedTabIndex = index
                                },
                                modifier = Modifier
                                    .weight(if (isSelected) 2f else 1f)
                            ) {
                                Text(
                                    text = if (isSelected) title else title.first().toString(),
                                    fontSize = if (isSelected) 16.sp else 12.sp,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            selectedTabIndex = selectedTab
                            onClose()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Red.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text("×", color = Color.White, fontSize = 24.sp)
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp)
                ) {
                    when (selectedTab) {
                        0 -> ProcessMenu(dialogCallback = this@OverlayService)
                        1 -> InitialMemoryMenu(this@OverlayService, dialogCallback = this@OverlayService)
                        2 -> AddressTableMenu(this@OverlayService, dialogCallback = this@OverlayService)
                        3 -> SettingsMenu()
                    }
                }
            }
        }
    }

    private fun showMenu() {
        if (!isMenuVisible) {
            updateMenuLayoutForCurrentOrientation()
            windowManager.addView(menuView, menuParams)
            isMenuVisible = true
        }
    }

    private fun hideMenu() {
        if (isMenuVisible) {
            windowManager.removeView(menuView)
            isMenuVisible = false
        }
    }

    override fun showInfoDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
        if (!isDialogVisible) {
            dialogView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    CustomOverlayInfoDialog(
                        title = title,
                        text = message,
                        onConfirm = onConfirm,
                        onDismiss = { hideDialog() }
                    )
                }
            }
            showDialogView()
        }
    }

    override fun showInputDialog(title: String, defaultValue: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
        if (!isDialogVisible) {
            dialogView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    CustomOverlayInputDialog(
                        title = title,
                        defaultValue = defaultValue,
                        onConfirm = onConfirm,
                        onDismiss = { hideDialog() }
                    )
                }
            }
            showDialogView()
        }
    }

    override fun showAddressDialog(title: String, onAddressDeleted: () -> Unit, onDismiss: () -> Unit) {
        if (!isDialogVisible) {
            dialogView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    CustomOverlayAddressDialog(
                        title = title,
                        onAddressDeleted = onAddressDeleted,
                        onDismiss = { hideDialog() }
                    )
                }
            }
            showDialogView()
        }
    }

    private fun showDialogView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(dialogView, params)
        isDialogVisible = true
    }

    private fun hideDialog() {
        if (isDialogVisible) {
            windowManager.removeView(dialogView)
            isDialogVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener.disable()

        lastKnownX = params.x
        lastKnownY = params.y

        if (::overlayView.isInitialized) {
            (overlayView.parent as? android.view.ViewGroup)?.let { parent ->
                windowManager.removeView(parent)
            }
        }
        if (::menuView.isInitialized && isMenuVisible) {
            windowManager.removeView(menuView)
        }
    }
}