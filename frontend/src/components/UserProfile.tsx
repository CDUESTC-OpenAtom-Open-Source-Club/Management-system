import React, { useState } from 'react'
import { Dropdown, Avatar, Modal, Form, Input, Button, Space, Typography, Tag, message } from 'antd'
import { UserOutlined, LogoutOutlined, KeyOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getCurrentUser, clearAuth, setCurrentUser } from '../utils/auth'
import { changePassword, getMe } from '../api/auth'
import type { MenuProps } from 'antd'

const { Text } = Typography

const UserProfile: React.FC = () => {
  const navigate = useNavigate()
  const user = getCurrentUser()
  const [pwdOpen, setPwdOpen] = useState(false)
  const [pwdLoading, setPwdLoading] = useState(false)
  const [form] = Form.useForm()

  const handleLogout = () => Modal.confirm({ title: '确认退出登录？', okText: '退出', cancelText: '取消', okButtonProps: { danger: true }, onOk: () => { clearAuth(); navigate('/login', { replace: true }) } })
  const handleChangePassword = async (values: { oldPassword: string; newPassword: string; confirmPassword: string }) => { if (values.newPassword !== values.confirmPassword) return void message.error('两次密码不一致'); setPwdLoading(true); try { await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword }); message.success('密码修改成功，请重新登录'); clearAuth(); setTimeout(() => navigate('/login', { replace: true }), 1000) } finally { setPwdLoading(false); setPwdOpen(false); form.resetFields() } }
  const refreshUser = async () => { const me = await getMe(); setCurrentUser(me); message.success('资料已刷新') }
  const menuItems: MenuProps['items'] = [{ key: 'profile', label: '我的资料', onClick: () => navigate('/my-profile') }, { type: 'divider' }, { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true, onClick: handleLogout }]

  return <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight"><Button type="text" style={{ height: 'auto', padding: 0 }}><Space size={10} align="center"><Avatar size={32} icon={<UserOutlined />} style={{ background: '#e5eefc', color: '#2f6bff' }} /><div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', lineHeight: 1.15 }}><Text style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>{user?.name || user?.username || '用户'}</Text><Space size={6} wrap>{user?.position && <Tag color="default" style={{ margin: 0 }}>{user.position}</Tag>}</Space></div></Space></Button></Dropdown>
}

export default UserProfile
