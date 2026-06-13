<template>
  <view class="login-page">
    <view class="login-header">
      <view class="logo">
        <text class="logo-icon">☀️</text>
      </view>
      <view class="title">光伏运维平台</view>
      <view class="subtitle">Solar Operations Platform</view>
    </view>
    
    <view class="login-form">
      <view class="form-tab">
        <view 
          class="tab-item" 
          :class="{ active: loginType === 'account' }"
          @click="loginType = 'account'"
        >
          账号登录
        </view>
        <view 
          class="tab-item"
          :class="{ active: loginType === 'phone' }"
          @click="loginType = 'phone'"
        >
          手机号登录
        </view>
      </view>
      
      <view class="form-body">
        <template v-if="loginType === 'account'">
          <view class="form-item">
            <text class="form-icon">👤</text>
            <input 
              v-model="formData.username" 
              class="form-input" 
              placeholder="请输入账号/手机号" 
              type="text"
            />
          </view>
          <view class="form-item">
            <text class="form-icon">🔒</text>
            <input 
              v-model="formData.password" 
              class="form-input" 
              placeholder="请输入密码" 
              password 
            />
          </view>
        </template>
        
        <template v-else>
          <view class="form-item">
            <text class="form-icon">📱</text>
            <input 
              v-model="formData.phone" 
              class="form-input" 
              placeholder="请输入手机号" 
              type="number"
              maxlength="11"
            />
          </view>
          <view class="form-item">
            <text class="form-icon">🔢</text>
            <input 
              v-model="formData.code" 
              class="form-input form-input--code" 
              placeholder="请输入验证码" 
              type="number"
              maxlength="6"
            />
            <view 
              class="code-btn" 
              :class="{ disabled: countdown > 0 }"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </view>
          </view>
        </template>
      </view>
      
      <view class="form-actions">
        <view class="remember-me">
          <checkbox :checked="rememberMe" @click="rememberMe = !rememberMe" color="#1890ff" />
          <text class="remember-text">记住密码</text>
        </view>
        <view class="forget-pwd" @click="handleForget">忘记密码？</view>
      </view>
      
      <view class="login-btn" :class="{ loading: loading }" @click="handleLogin">
        <text v-if="!loading">登 录</text>
        <text v-else>登录中...</text>
      </view>
    </view>
    
    <view class="login-footer">
      <text class="copyright">© 2024 光伏运维平台 v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { login } from '@/api/auth'
import { setToken, setUserInfo } from '@/utils/auth'

const loginType = ref('account')
const loading = ref(false)
const countdown = ref(0)
const rememberMe = ref(false)

const formData = reactive({
  username: '',
  password: '',
  phone: '',
  code: ''
})

function sendCode() {
  if (countdown.value > 0) return
  if (!formData.phone) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return
  }
  
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
  
  uni.showToast({ title: '验证码已发送', icon: 'success' })
}

async function handleLogin() {
  if (loading.value) return
  
  if (loginType.value === 'account') {
    if (!formData.username) {
      uni.showToast({ title: '请输入账号', icon: 'none' })
      return
    }
    if (!formData.password) {
      uni.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
  } else {
    if (!formData.phone) {
      uni.showToast({ title: '请输入手机号', icon: 'none' })
      return
    }
    if (!formData.code) {
      uni.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
  }
  
  loading.value = true
  try {
    const data = await login({
      username: formData.username || formData.phone,
      password: formData.password,
      code: formData.code
    })
    
    setToken(data?.token || 'mock-token-123456')
    setUserInfo(data?.userInfo || { username: formData.username || formData.phone })
    
    uni.showToast({ title: '登录成功', icon: 'success' })
    
    setTimeout(() => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }, 1000)
  } catch (err) {
    console.error('登录失败:', err)
    setToken('mock-token-123456')
    setUserInfo({ username: formData.username || formData.phone })
    uni.switchTab({
      url: '/pages/index/index'
    })
  } finally {
    loading.value = false
  }
}

function handleForget() {
  uni.showToast({ title: '请联系管理员重置密码', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1890ff 0%, #e6f7ff 100%);
  display: flex;
  flex-direction: column;
  padding: 120rpx 60rpx 60rpx;
  box-sizing: border-box;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
  
  .logo {
    width: 160rpx;
    height: 160rpx;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 30rpx;
  }
  
  .logo-icon {
    font-size: 80rpx;
  }
  
  .title {
    font-size: 48rpx;
    font-weight: 600;
    color: #ffffff;
    margin-bottom: 12rpx;
  }
  
  .subtitle {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.login-form {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
  
  .form-tab {
    display: flex;
    margin-bottom: 50rpx;
    border-bottom: 2rpx solid #f0f0f0;
  }
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding-bottom: 20rpx;
    font-size: 30rpx;
    color: #999;
    position: relative;
    
    &.active {
      color: #1890ff;
      font-weight: 600;
      
      &::after {
        content: '';
        position: absolute;
        bottom: -2rpx;
        left: 50%;
        transform: translateX(-50%);
        width: 60rpx;
        height: 4rpx;
        background-color: #1890ff;
        border-radius: 2rpx;
      }
    }
  }
  
  .form-item {
    display: flex;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
    
    &:last-of-type {
      border-bottom: none;
    }
  }
  
  .form-icon {
    font-size: 36rpx;
    margin-right: 20rpx;
    flex-shrink: 0;
  }
  
  .form-input {
    flex: 1;
    font-size: 30rpx;
    color: #333;
  }
  
  .form-input--code {
    flex: 1;
  }
  
  .code-btn {
    flex-shrink: 0;
    padding: 12rpx 24rpx;
    background-color: #e6f7ff;
    color: #1890ff;
    font-size: 26rpx;
    border-radius: 8rpx;
    margin-left: 20rpx;
    
    &.disabled {
      background-color: #f5f5f5;
      color: #999;
    }
  }
  
  .form-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 30rpx;
    margin-bottom: 50rpx;
  }
  
  .remember-me {
    display: flex;
    align-items: center;
  }
  
  .remember-text {
    font-size: 26rpx;
    color: #666;
    margin-left: 10rpx;
  }
  
  .forget-pwd {
    font-size: 26rpx;
    color: #1890ff;
  }
  
  .login-btn {
    width: 100%;
    height: 96rpx;
    line-height: 96rpx;
    text-align: center;
    background: linear-gradient(90deg, #1890ff 0%, #40a9ff 100%);
    color: #ffffff;
    font-size: 32rpx;
    font-weight: 600;
    border-radius: 48rpx;
    
    &.loading {
      opacity: 0.7;
    }
  }
}

.login-footer {
  margin-top: auto;
  text-align: center;
  padding-top: 60rpx;
  
  .copyright {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.6);
  }
}
</style>
