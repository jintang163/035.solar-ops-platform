const permissionUtil = {
  requestCameraPermission() {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      if (plus.os.name === 'Android') {
        plus.android.requestPermissions(
          ['android.permission.CAMERA', 'android.permission.WRITE_EXTERNAL_STORAGE'],
          (e) => {
            if (e.granted && e.granted.length > 0) {
              resolve(true)
            } else {
              reject(new Error('相机权限被拒绝'))
            }
          },
          (e) => {
            reject(e)
          }
        )
      } else {
        resolve(true)
      }
      // #endif
      
      // #ifdef MP-WEIXIN
      wx.authorize({
        scope: 'scope.camera',
        success: () => resolve(true),
        fail: () => reject(new Error('相机权限被拒绝'))
      })
      // #endif
      
      // #ifdef H5
      resolve(true)
      // #endif
    })
  },

  requestAudioPermission() {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      if (plus.os.name === 'Android') {
        plus.android.requestPermissions(
          ['android.permission.RECORD_AUDIO', 'android.permission.WRITE_EXTERNAL_STORAGE'],
          (e) => {
            if (e.granted && e.granted.length > 0) {
              resolve(true)
            } else {
              reject(new Error('录音权限被拒绝'))
            }
          },
          (e) => {
            reject(e)
          }
        )
      } else {
        resolve(true)
      }
      // #endif
      
      // #ifdef MP-WEIXIN
      wx.authorize({
        scope: 'scope.record',
        success: () => resolve(true),
        fail: () => reject(new Error('录音权限被拒绝'))
      })
      // #endif
      
      // #ifdef H5
      resolve(true)
      // #endif
    })
  },

  requestLocationPermission() {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      if (plus.os.name === 'Android') {
        plus.android.requestPermissions(
          ['android.permission.ACCESS_FINE_LOCATION', 'android.permission.ACCESS_COARSE_LOCATION'],
          (e) => {
            if (e.granted && e.granted.length > 0) {
              resolve(true)
            } else {
              reject(new Error('定位权限被拒绝'))
            }
          },
          (e) => {
            reject(e)
          }
        )
      } else {
        resolve(true)
      }
      // #endif
      
      // #ifdef MP-WEIXIN
      wx.authorize({
        scope: 'scope.userLocation',
        success: () => resolve(true),
        fail: () => reject(new Error('定位权限被拒绝'))
      })
      // #endif
      
      // #ifdef H5
      resolve(true)
      // #endif
    })
  },

  requestStoragePermission() {
    return new Promise((resolve, reject) => {
      // #ifdef APP-PLUS
      if (plus.os.name === 'Android') {
        plus.android.requestPermissions(
          ['android.permission.WRITE_EXTERNAL_STORAGE', 'android.permission.READ_EXTERNAL_STORAGE'],
          (e) => {
            if (e.granted && e.granted.length > 0) {
              resolve(true)
            } else {
              reject(new Error('存储权限被拒绝'))
            }
          },
          (e) => {
            reject(e)
          }
        )
      } else {
        resolve(true)
      }
      // #endif
      
      // #ifndef APP-PLUS
      resolve(true)
      // #endif
    })
  },

  checkAllPermissions() {
    return Promise.all([
      this.requestCameraPermission().catch(() => false),
      this.requestAudioPermission().catch(() => false),
      this.requestLocationPermission().catch(() => false),
      this.requestStoragePermission().catch(() => false)
    ]).then(results => {
      return {
        camera: results[0],
        audio: results[1],
        location: results[2],
        storage: results[3]
      }
    })
  },

  gotoAppSettings() {
    // #ifdef APP-PLUS
    if (plus.os.name === 'Android') {
      const main = plus.android.runtimeMainActivity()
      const Intent = plus.android.importClass('android.content.Intent')
      const Settings = plus.android.importClass('android.provider.Settings')
      const Uri = plus.android.importClass('android.net.Uri')
      const intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      const uri = Uri.fromParts('package', main.getPackageName(), null)
      intent.setData(uri)
      main.startActivity(intent)
    }
    // #endif
  }
}

export default permissionUtil
