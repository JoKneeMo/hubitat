# AdGuard Home DNS Manager (Unofficial) #

This package is not supported nor endorsed by AdGuard!

This package will allow you to manage the global services of an AdGuard Home DNS server and it's configured clients.
A main application allows creation of multiple servers within Hubitat, and the clients are created as child devices of each server.


## Application ##

- [AdGuardHome_App.groovy](https://github.com/JoKneeMo/hubitat/adguardhome/blob/master/app/AdGuardHome_App.groovy) - Not required but makes managing multiple servers easier

## Main Driver ##

- [adGuardHome_Server.groovy](https://github.com/JoKneeMo/hubitat/adguardhome/blob/master/drivers/AdGuardHome_Server.groovy) - Required, this is the server device and handles the authentication and communication for each server.

## Client Drivers ##

- [adGuardHome_Client.groovy](https://github.com/JoKneeMo/hubitat/adguardhome/blob/master/drivers/AdGuardHome_Client.groovy) - Not required, this is the client device and is only needed if you want to manage individual client settings.