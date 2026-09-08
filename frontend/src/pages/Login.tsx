import React, { useState } from 'react'
import { Form, Input, Button, message, Typography } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'
import { login } from '../api/auth'
import { setToken, setCurrentUser } from '../utils/auth'
import LoginStarfield from '../components/LoginStarfield'
import logo from '../assets/logo.png'
import '../styles/login.css'

const { Title, Text } = Typography

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as any)?.from?.pathname || '/'

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const res = await login(values)
      setToken(res.token)
      setCurrentUser(res.user)
      const needSetup = res.user.profileCompleted === false || res.user.initialPasswordChanged === false
      if (res.user.initialPasswordChanged === false) {
        message.warning('请先修改初始密码')
      } else {
        message.success(`欢迎回来，${res.user.name || res.user.username}！`)
      }
      navigate(needSetup ? '/my-profile' : (from || '/'), { replace: true })
    } catch {
      // 错误已由 request.ts 拦截器处理
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <LoginStarfield />
      <div className="login-card">
        <div className="login-brand">
          <div className="login-logo-wrap">
            <img className="login-logo" src={logo} alt="开放原子开源社团 logo" />
          </div>
          <Title className="login-title" level={1}>
            开放原子开源社团
          </Title>
          <Text className="login-subtitle">秘书处资料与积分管理系统</Text>
          <Text className="login-hint">请输入账号和密码登录系统</Text>
        </div>

        <Form
          className="login-form"
          name="login"
          onFinish={handleLogin}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              className="login-input"
              prefix={<UserOutlined />}
              placeholder="用户名"
              autoComplete="off"
              data-lpignore="true"
              data-1p-ignore="true"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              className="login-input"
              prefix={<LockOutlined />}
              placeholder="密码"
              autoComplete="new-password"
              data-lpignore="true"
              data-1p-ignore="true"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, marginTop: 10 }}>
            <Button
              className="login-button"
              type="primary"
              htmlType="submit"
              loading={loading}
              block
            >
              {loading ? '登录中...' : '登 录'}
            </Button>
          </Form.Item>
        </Form>

        <div className="login-footer">
          <Text className="login-footer-text">如需账号请联系系统管理员</Text>
        </div>
      </div>
    </div>
  )
}

export default Login
