import React, { useEffect, useState } from 'react'
import { Button, Card, Form, Input, Select, message, Descriptions, Space, Spin, Typography, Alert, Divider } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getMyProfile, updateMyProfile } from '../api/profile'
import { changePassword } from '../api/auth'
import { getCurrentUser, setCurrentUser, clearAuth } from '../utils/auth'
import type { MyProfileResponse, UpdateMyProfileRequest } from '../api/auth'
import { cohortLabel } from '../utils/cohort'

const { Title, Text } = Typography

// 普通成员可自行选择的部门（管理员部门「秘书处」不可自选，后端二次校验兜底）
const SELF_SELECTABLE_DEPARTMENTS = ['技术部', '外联部', '宣策部', '组织部']
// 普通成员可自行选择的职务（管理员职务「部长/副会长/会长」不可自选）
const SELF_SELECTABLE_POSITIONS = ['社员']

const MyProfile: React.FC = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [profile, setProfile] = useState<MyProfileResponse | null>(null)
  const [changingPassword, setChangingPassword] = useState(false)
  const [passwordForm] = Form.useForm<{ oldPassword: string; newPassword: string; confirmPassword: string }>()
  const navigate = useNavigate()
  const currentUser = getCurrentUser()

  useEffect(() => {
    void loadProfile()
  }, [])

  const loadProfile = async () => {
    setLoading(true)
    try {
      const res = await getMyProfile()
      setProfile(res)
      form.setFieldsValue({
        name: res.name,
        studentNo: res.studentNo,
        phone: res.phone,
        major: res.major,
        department: res.department,
        position: res.position,
      })
    } finally {
      setLoading(false)
    }
  }

  const handleChangePassword = async (values: { oldPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的新密码不一致')
      return
    }
    setChangingPassword(true)
    try {
      await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword })
      message.success('密码修改成功，请重新登录')
      passwordForm.resetFields()
      setTimeout(() => {
        clearAuth()
        navigate('/login', { replace: true })
      }, 800)
    } finally {
      setChangingPassword(false)
    }
  }

  const handleFinish = async (values: UpdateMyProfileRequest) => {
    setSaving(true)
    const wasIncomplete = profile?.profileCompleted === false
    try {
      const res = await updateMyProfile(values)
      const nextUser = {
        ...(currentUser || {}),
        ...res,
        profileCompleted: true,
        fullAccess: res.fullAccess,
      }
      setCurrentUser(nextUser as ReturnType<typeof getCurrentUser> extends infer T ? NonNullable<T> : never)
      setProfile(res)
      message.success('资料保存成功')
      if (wasIncomplete) {
        navigate('/', { replace: true })
      } else {
        await loadProfile()
      }
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Card>
        <Spin />
      </Card>
    )
  }

  // 部门/职务可选项：始终包含当前值（避免已持有管理员身份的用户被迫改动），再叠加可自选的非管理员值
  const departmentOptions = Array.from(new Set([
    ...(profile?.department ? [profile.department] : []),
    ...SELF_SELECTABLE_DEPARTMENTS,
  ])).map((d) => ({ label: d, value: d }))
  const positionOptions = Array.from(new Set([
    ...(profile?.position ? [profile.position] : []),
    ...SELF_SELECTABLE_POSITIONS,
  ])).map((p) => ({ label: p, value: p }))

  return (
    <Card>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>我的资料</Title>
          {profile?.initialPasswordChanged === false && (
            <Alert
              type="warning"
              showIcon
              message="首次登录请修改初始密码"
              description="检测到您正在使用初始密码，为了账号安全，请先修改密码再完善资料。"
              style={{ marginBottom: 16, marginTop: 12 }}
            />
          )}
          {profile?.profileCompleted === false && profile?.initialPasswordChanged !== false && (
            <Text type="warning">请先完善个人资料，以便秘书处进行成员管理和积分统计。</Text>
          )}
        </div>

        {/* 密码修改区域 */}
        <Card size="small" title="修改密码" style={{ maxWidth: 720 }}>
          <Form
            form={passwordForm}
            layout="vertical"
            onFinish={handleChangePassword}
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
            <Form.Item style={{ marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" loading={changingPassword}>
                修改密码
              </Button>
            </Form.Item>
          </Form>
        </Card>

        <Divider />

        {/* 届次（只读，由管理员在成员管理中维护） */}
        <Card size="small" title="组织身份" style={{ maxWidth: 720 }}>
          <Descriptions column={1} size="small">
            <Descriptions.Item label="届次">{cohortLabel(profile?.cohortYear)}</Descriptions.Item>
          </Descriptions>
          <Text type="secondary">届次由管理员在「成员管理」中维护，本人不可修改；部门、职务可在下方自行选择。</Text>
        </Card>

        {/* 个人资料（仅本人可维护） */}
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          style={{ maxWidth: 720 }}
        >
          <Form.Item label="姓名" name="name" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item label="学号" name="studentNo" rules={[{ required: true, message: '请输入学号' }]}>
            <Input placeholder="请输入学号" />
          </Form.Item>
          <Form.Item label="手机号" name="phone">
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item label="专业" name="major">
            <Input placeholder="请输入专业" />
          </Form.Item>
          <Form.Item label="部门" name="department">
            <Select placeholder="请选择部门" options={departmentOptions} />
          </Form.Item>
          <Form.Item label="职务" name="position">
            <Select placeholder="请选择职务" options={positionOptions} />
          </Form.Item>
          <Space>
            <Button onClick={() => navigate(-1)}>返回</Button>
            <Button type="primary" htmlType="submit" loading={saving}>保存资料</Button>
          </Space>
        </Form>
      </Space>
    </Card>
  )
}

export default MyProfile
