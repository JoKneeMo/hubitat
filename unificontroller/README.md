# UniFi Controller Manager #

This package is not supported nor endorsed by Ubiquiti!

This package will allow you to manage a manage a UniFi Controller and child drivers on Hubitat.
The motivation behind this was to serve camera snapshots from the dashboard, but can also support future UniFi integrations without having to configure the controller in multiple devices.

This HPM app would not exist without the work by [tomw](https://community.hubitat.com/u/tomw) in the Hubitat forums!

## Application ##

- [UniFiController_App.groovy](https://github.com/JoKneeMo/hubitat/blob/main/unificontroller/app/UniFiController_App.groovy) - Required, it's the main app that manages controllers and serves images for the dashboard to use.

## Drivers ##

- [UniFi Protect Controller](https://raw.githubusercontent.com/tomwpublic/hubitat_unifiProtect/main/unifiProtectController) - Required, manages communication for UniFi Protect devices. Written by [tomw](https://github.com/tomwpublic/hubitat_unifiProtect/) and only imported to this package. Any updates or licensing are on his end, this HPM app just references it!

- [UniFi Protect Camera](https://raw.githubusercontent.com/tomwpublic/hubitat_unifiProtect/main/unifiProtectCamera) - Required by UniFi Protect Controller, used for the camera handling. Written by [tomw](https://github.com/tomwpublic/hubitat_unifiProtect/) and only imported to this package. Any updates or licensing are on his end, this HPM app just references it!

- [UniFi Protect Doorbell](https://raw.githubusercontent.com/tomwpublic/hubitat_unifiProtect/main/unifiProtectCamera) - Required by UniFi Protect Controller, used for the doorbell action handling. Written by [tomw](https://github.com/tomwpublic/hubitat_unifiProtect/) and only imported to this package. Any updates or licensing are on his end, this HPM app just references it!
