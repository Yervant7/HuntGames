# HuntGames

HuntGames is an open-source project aimed at providing tools for game hunting and analysis on ARM64 devices. This project uses the kernel for Read and Write memory

## Features

- Disassemble functions for better analysis.
- Write assembly code directly.
- Search values in memory of process.
- Write values in memory of process.

## Requirements

- ARM64 device
- Root access
- Compiled kernel module (refer to the [rwMem repository](https://github.com/Yervant7/rwMem))
- Unlocked bootloader

## Installation

1. Download apk in the realeases section

2. Compile the necessary kernel module from the [rwMem repository](https://github.com/Yervant7/rwMem):
    Instructions in the repository see too [Kernel_Action repository](https://github.com/Yervant7/Kernel_Action)
 
3. Load the compiled module:
    ```bash
    su -c insmod rwmem.ko
    ```

## Usage

To start using HuntGames, ensure that you have met all the requirements and loaded the kernel module. Follow the instructions to enable the global namespace in your root implementation.

## Community and Support

For discussions and support, join our Telegram group: [HuntGames Telegram Group](https://t.me/huntgames7).

Watch the [demo video](https://youtu.be/hMQYwH0Hmcs?si=OB-4-XjnJ-mDBt1z) to see HuntGames in action.

## Contributing

We welcome contributions! Please fork the repository, create a feature branch, and submit a pull request. Ensure your code adheres to the project's coding standards and passes all tests.

## Acknowledgments

- Inspiration and guidance from the
ri-char: https://github.com/ri-char
abcz316: https://github.com/abcz316
for the Kernel Module Read and Write to Memory

KuhakuPixel: https://github.com/KuhakuPixel
for the ui

---

*Note: The project is in its early stages. Please report any issues or bugs in the Issues section.*
