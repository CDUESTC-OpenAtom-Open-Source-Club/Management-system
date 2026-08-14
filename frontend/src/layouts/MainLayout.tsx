import React, { useMemo, useState } from 'react'
import { Avatar, Button, Layout, Menu, Space, Tag, theme, Typography, Dropdown, Modal, Form, Input, message } from 'antd'
import type { MenuProps } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  HomeOutlined,
  TeamOutlined,
  TrophyOutlined,
  FormOutlined,
  CheckCircleOutlined,
  TableOutlined,
  FolderOpenOutlined,
  FileTextOutlined,
  AccountBookOutlined,
  AuditOutlined,
  IdcardOutlined,
  UserOutlined,
  DownOutlined,
  KeyOutlined,
  BookOutlined,
  EditOutlined,
  FileSearchOutlined,
  CalendarOutlined,
} from '@ant-design/icons'
import { changePassword } from '../api/auth'
import { canManage, canViewFinance, canViewLogs, isFullAccess, canManageHomework } from '../utils/permission'
import { getCurrentUser, clearAuth } from '../utils/auth'
import logo from '../assets/logo.png'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const MainLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  const [passwordForm] = Form.useForm<{ oldPassword: string; newPassword: string; confirmPassword: string }>()
  const { token: { colorBgContainer } } = theme.useToken()
  const currentUser = getCurrentUser()

  const items = useMemo(() => ([
    { key: '/', icon: <HomeOutlined />, label: '首页概览' },
    { key: '/members', icon: <TeamOutlined />, label: '成员管理' },
    { key: '/point-items', icon: <TrophyOutlined />, label: '积分项目管理', show: canManage() },
    { key: '/my-applications', icon: <FormOutlined />, label: '我的活动登记' },
    { key: '/point-applications', icon: <CheckCircleOutlined />, label: '积分审核', show: canManage() },
    { key: '/points-table', icon: <TableOutlined />, label: '积分总表' },
    { key: '/my-homework', icon: <BookOutlined />, label: '我的作业' },
    { key: '/homework-review', icon: <EditOutlined />, label: '作业批改', show: canManageHomework() },
    { key: '/homework-management', icon: <FileSearchOutlined />, label: '作业管理', show: canManageHomework() },
    { key: '/archive-links', icon: <FolderOpenOutlined />, label: '活动资料归档' },
    { key: '/meeting-minutes', icon: <FileTextOutlined />, label: '会议纪要' },
    { key: '/finance', icon: <AccountBookOutlined />, label: '财务台账', show: canViewFinance() },
    { key: '/operation-logs', icon: <AuditOutlined />, label: '操作日志', show: canViewLogs() },
    { key: '/my-profile', icon: <IdcardOutlined />, label: '我的资料' },
    { key: '/users', icon: <UserOutlined />, label: '账号管理', show: isFullAccess() },
    { key: '/cohorts', icon: <CalendarOutlined />, label: '届次管理', show: isFullAccess() },
  ].filter((item) => item.show === undefined || item.show)), [])

  const selectedKey = items.find((i) => i.key !== '/' && location.pathname.startsWith(i.key))?.key ?? location.pathname

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

  const userMenu: MenuProps['items'] = [
    { key: 'profile', label: '我的资料', icon: <IdcardOutlined />, onClick: () => navigate('/my-profile') },
    { key: 'password', label: '修改密码', icon: <KeyOutlined />, onClick: () => { passwordForm.resetFields(); setPasswordModalOpen(true) } },
    { type: 'divider' },
    { key: 'logout', label: '退出登录', onClick: () => { clearAuth(); navigate('/login', { replace: true }) } },
  ]

  return (
    <Layout className="app-shell" style={{ minHeight: '100vh' }}>
      <Sider className="app-sider" collapsible collapsed={collapsed} onCollapse={setCollapsed} width={220}>
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
        <Menu theme="dark" mode="inline" selectedKeys={[selectedKey]} items={items} onClick={({ key }) => navigate(key)} />
      </Sider>

      <Layout>
        <Header className="app-header">
          <div className="app-header-title">
            <div className="app-header-title-main">工作台</div>
            <div className="app-header-title-sub">成员管理、积分登记、资料归档与日常办公</div>
          </div>

          <Dropdown menu={{ items: userMenu }} trigger={['click']} placement="bottomRight">
            <div className="app-header-user">
              <Avatar size={32} icon={<UserOutlined />} />
              <div className="app-header-user-meta">
                <Text className="app-header-username">{currentUser?.name || currentUser?.username || '用户'}</Text>
                <Space size={6} wrap>
                  {currentUser?.position && <Tag className="app-header-tag">{currentUser.position}</Tag>}
                  <DownOutlined className="app-header-arrow" />
                </Space>
              </div>
            </div>
          </Dropdown>
        </Header>

        <Content className="app-content">
          <div style={{ background: colorBgContainer, borderRadius: 12, padding: 0 }}>
            <Outlet />
          </div>
        </Content>
      </Layout>

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
