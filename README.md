# vPlayer
An unofficial Android TV app for watching videos from VRT NU.

It requires that your account has a specific password for VRT NU, social logins have not been implemented.  

This app just renders the data from the site in an Android TV specific way. Your credentials never leave your device.  

## Download and Install
The lastest version can be downloaded [from gitlab](https://gitlab.com/wdullaer/vplayer/-/jobs/artifacts/master/download?job=apk) at any time.  
You will need to install it by sideloading it onto your device. A play store version will not be provided because the VRT seems to send takedown notices to any apps that see some success, and I don't feel like dealing with that.

## TODO
* Return errors from VrtData functions and render them properly
* Add unit tests to VrtData functions
* Add livestreams
* Implement multiple authentication methods (requires using the gigya library, not happening in the short term)
