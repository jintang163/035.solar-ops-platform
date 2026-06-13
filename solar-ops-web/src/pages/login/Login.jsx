import React, { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login as loginApi } from '../../api/login'
import { setToken, setUser } from '../../utils/auth'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const res = await loginApi(values)
      if (res.code === 200 || res.code === 0) {
        const { token, userInfo } = res.data
        setToken(token)
        setUser(userInfo)
        message.success('登录成功')
        navigate('/dashboard')
      }
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <div className="login-bg" />
      <Card className="login-card" bordered={false}>
        <div className="login-logo">
          <span className="logo-icon">☀</span>
          <h1 className="login-title">太阳能运维管理平台</h1>
          <p className="login-subtitle">Solar Operations Management Platform</p>
        </div>
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="请输入用户名"
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              className="login-btn"
            >
              登 录
            </Button>
          </Form.Item>
        </Form>
        <div className="login-footer">
          <p>© 2024 太阳能运维平台 版权所有</p>
        </div>
      </Card>
    </div>
  )
}

export default Login
