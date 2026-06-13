<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { getToken } from '@/utils/auth'

onLaunch(() => {
  console.log('App Launch')
  checkLogin()
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})

function checkLogin() {
  const token = getToken()
  if (!token) {
    uni.redirectTo({
      url: '/pages/login/login'
    })
  }
}
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
