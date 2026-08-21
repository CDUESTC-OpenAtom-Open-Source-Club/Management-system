import React, { useEffect, useMemo, useState } from 'react'
import { Avatar, Button, Layout, Menu, Space, Typography, Dropdown, Modal, Form, Input, message } from 'antd'
import type { MenuProps } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  UserOutlined,
  DownOutlined,
  KeyOutlined,
  IdcardOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import { changePassword } from '../api/auth'
import { getCurrentUser, clearAuth } from '../utils/auth'
import logo from '../assets/logo.png'
import { NAV_GROUPS, visibleNavItems, NavItem } from '../config/navigation'
import CommandPalette from '../components/CommandPalette'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const MainLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  const [paletteOpen, setPaletteOpen] = useState(false)
  const [passwordForm] = Form.useForm<{ oldPassword: string; newPassword: string; confirmPassword: string }>()
  const currentUser = getCurrentUser()

  const menuItems: MenuProps['items'] = useMemo(
    () =>
      NAV_GROUPS.map((group) => {
        const children = group.items
          .filter((item) => !item.show || item.show())
          .map((item) => ({ key: item.key, icon: item.icon, label: item.label }))
        return {
          type: 'group' as const,
          key: `group-${group.key}`,
          label: group.label,
          children,
        }
      }).filter((group) => group.children.length > 0),
    [],
  )

  const flatItems = useMemo(() => visibleNavItems(), [])
  const selectedKey = useMemo(
    () => flatItems.find((i) => i.key !== '/' && location.pathname.startsWith(i.key))?.key ?? location.pathname,
    [flatItems, location.pathname],
  )

  const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform || '')

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault()
        setPaletteOpen(true)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  const handleChangePassword = async (values: { oldPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的新密码不一致')
      return
    }
    setChangingPassword(true)
    try {
      await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword })
      message.success('密码修改成功，请重新登录')
      setPasswordModalOpen(false)
      passwordForm.resetFields()
      setTimeout(() => {
        clearAuth()
        navigate('/login', { replace: true })
      }, 800)
    } finally {
      setChangingPassword(false)
    }
  }

  const handleOpenAList = () => {
    const url = import.meta.env.VITE_ALIST_URL?.trim()
    if (!url) {
      message.warning('网盘地址未配置，请联系管理员')
      return
    }
    try {
      const parsed = new URL(url)
      if (!['http:', 'https:'].includes(parsed.protocol)) {
        message.error('网盘地址配置无效')
        return
      }
      window.open(url, '_blank', 'noopener,noreferrer')
    } catch {
      message.error('网盘地址配置无效')
    }
  }

  const handlePaletteNavigate = (item: NavItem) => {
    setPaletteOpen(false)
    if (item.key === 'alist') {
      handleOpenAList()
    } else {
      navigate(item.key)
    }
  }

  const userMenu: MenuProps['items'] = [
    { key: 'profile', label: '我的资料', icon: <IdcardOutlined />, onClick: () => navigate('/my-profile') },
    { key: 'password', label: '修改密码', icon: <KeyOutlined />, onClick: () => { passwordForm.resetFields(); setPasswordModalOpen(true) } },
    { type: 'divider' },
    { key: 'logout', label: '退出登录', onClick: () => { clearAuth(); navigate('/login', { replace: true }) } },
  ]

  return (
    <Layout className="app-shell" style={{ minHeight: '100vh' }}>
      <Sider className="app-sider" collapsible collapsed={collapsed} onCollapse={setCollapsed} width={224} breakpoint="lg">
        <div className="app-brand">
          <div className="app-brand-logo-wrap">
            <img className="app-brand-logo" src={logo} alt="开放原子开源社团" />
          </div>
          {!collapsed && (
            <div className="app-brand-text">
              <div className="app-brand-title">开放原子开源社团</div>
              <div className="app-brand-subtitle">秘书处管理系统</div>
            </div>
          )}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => {
            if (key === 'alist') {
              handleOpenAList()
              return
            }
            navigate(key)
          }}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <div className="app-header-title">
            <div className="app-header-title-main">工作台</div>
          </div>

          <div className="app-header-right">
            <button type="button" className="app-header-search" onClick={() => setPaletteOpen(true)} aria-label="搜索功能或页面">
              <SearchOutlined />
              <span className="app-header-search-text">搜索功能或页面…</span>
              <span className="app-header-search-hint">{isMac ? '⌘ K' : 'Ctrl K'}</span>
            </button>

            <Dropdown menu={{ items: userMenu }} trigger={['click']} placement="bottomRight">
              <div className="app-header-user" role="button" tabIndex={0} aria-label="用户菜单">
                <Avatar size={32} icon={<UserOutlined />} style={{ background: '#2f6bff' }} />
                <div className="app-header-user-meta">
                  <Text className="app-header-username">{currentUser?.name || currentUser?.username || '用户'}</Text>
                  <span className="app-header-position">{currentUser?.position || '成员'}</span>
                </div>
                <DownOutlined className="app-header-arrow" />
              </div>
            </Dropdown>
          </div>
        </Header>

        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>

      <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} onNavigate={handlePaletteNavigate} />

      <Modal
        title="修改密码"
        open={passwordModalOpen}
        onCancel={() => setPasswordModalOpen(false)}
        footer={null}
        destroyOnClose
        width={420}
      >
        <Form
          form={passwordForm}
          layout="vertical"
          onFinish={handleChangePassword}
          style={{ marginTop: 12 }}
        >
          <Form.Item
            label="当前密码"
            name="oldPassword"
            rules={[{ required: true, message: '请输入当前密码' }]}
          >
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>
          <Form.Item
            label="新密码"
            name="newPassword"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '新密码至少 6 位' },
            ]}
          >
            <Input.Password placeholder="请输入新密码（至少 6 位）" />
          </Form.Item>
          <Form.Item
            label="确认新密码"
            name="confirmPassword"
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的新密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setPasswordModalOpen(false)}>取消</Button>
              <Button type="primary" htmlType="submit" loading={changingPassword}>
                确认修改
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  )
}

export default MainLayout
