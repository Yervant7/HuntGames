-- Set up memory regions and value type
-- HG_set_regions("C_ALLOC,C_BSS,C_DATA,C_HEAP,JAVA_HEAP,A_ANONYMOUS,STACK,ASHMEM")
-- HG_set_valuetype("int") -- valuetype: int, long, float, double

-- Navigate to specific memory addresses
-- HG_goto_address("0x12345678")
-- HG_goto_address_offset("0x12345678+0x10")

-- Search for a value and read memory content
-- HG_search_value("42")
-- local value_at_address = HG_read_mem("0x12345678")
-- print("Value at address 0x12345678: " .. tostring(value_at_address))

-- Write and freeze values in memory
-- HG_write_value("100", "0x12345678")
-- HG_freeze_value("100", "0x12345678")

-- UI setup: add components to the menu
MenuManager_addCheckbox("Enable Feature", true)
MenuManager_addSlider(0.5)
MenuManager_addTextField("Enter text here")
MenuManager_addButton("Run Action", function() print("Button pressed") end)
MenuManager_addText("Status: Active")
MenuManager_setBackgroundColor(0xFF00FF)  -- Set background color to a magenta-like color

-- Callback functions
local function checkbox_callback(is_checked)
    print("Checkbox is now " .. (is_checked and "checked" or "unchecked"))
end

local function slider_callback(value)
    print("Slider value changed to " .. tostring(value))
end

local function textfield_callback(text)
    print("Text field updated with: " .. text)
end

-- Assign callbacks to the respective UI components
MenuManager_setCheckboxCallback(0, checkbox_callback)  -- Index 0 corresponds to the first checkbox added
MenuManager_setSliderCallback(0, slider_callback)      -- Index 0 corresponds to the first slider added
MenuManager_setTextFieldCallback(0, textfield_callback) -- Index 0 corresponds to the first text field added
