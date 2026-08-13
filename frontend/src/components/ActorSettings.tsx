import React, { useState } from 'react'
import { Modal, Form, Input, Select, Button, Typography, Space } from 'antd'
import { UserOutlined, SettingOutlined } from '@ant-design/icons'
import { getActor, setActor, type ActorInfo } from '../utils/actor'

const { Text } = Typography

const POSITION_OPTIONS = ['社员', '部长', '会长', '副会长']
const DEPARTMENT_OPTIONS = ['秘书处', '技术部', '宣传部', '运营部', '其他']

interface Props {
  onActorChange?: () => void
}

const ActorSettings: React.FC<Props> = ({ onActorChange }) => {
  const [open, setOpen] = useState(false)
  const [actor, setActorState] = useState<ActorInfo>(getActor())
  const [form] = Form.useForm<ActorInfo>()

  const handleOpen = () => {
    form.setFieldsValue(actor)
    setOpen(true)
  }

  const handleSave = () => {
    form.validateFields().then((values) => {
      setActor(values)
      setActorState(values)
      setOpen(false)
      onActorChange?.()
    })
  }

  const positionColor: Record<string, string> = {
    会长: '#f50',
    副会长: '#fa8c16',
    部长: '#1677ff',
    社员: '#52c41a',
  }

  return (
    <>
      <Space
        style={{ cursor: 'pointer' }}
        onClick={handleOpen}
      >
        <UserOutlined style={{ fontSize: 16, color: '#fff' }} />
        <Text style={{ color: '#fff', fontSize: 14 }}>
          {actor.name}
        </Text>
        <Text
          style={{
            color: positionColor[actor.position] || '#fff',
            fontSize: 12,
            background: 'rgba(255,255,255,0.15)',
            padding: '2px 8px',
            borderRadius: 10,
          }}
        >
          {actor.department} · {actor.position}
        </Text>
        <SettingOutlined style={{ color: 'rgba(255,255,255,0.7)', fontSize: 13 }} />
      </Space>

      <Modal
        title="设置当前身份"
        open={open}
        onOk={handleSave}
        onCancel={() => setOpen(false)}
        okText="保存"
        cancelText="取消"
        width={440}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="姓名"
            name="name"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item
            label="部门"
            name="department"
            rules={[{ required: true, message: '请输入或选择部门' }]}
          >
            <Select
              showSearch
              allowClear
              placeholder="请选择或输入部门"
              options={DEPARTMENT_OPTIONS.map((d) => ({ label: d, value: d }))}
              mode={undefined}
            />
          </Form.Item>
          <Form.Item
            label="职务"
            name="position"
            rules={[{ required: true, message: '请选择职务' }]}
          >
            <Select
              placeholder="请选择职务"
              options={POSITION_OPTIONS.map((p) => ({ label: p, value: p }))}
            />
          </Form.Item>
        </Form>
        <div style={{ padding: '8px 0', color: '#888', fontSize: 12 }}>
          ⚠️ 身份信息保存到本地浏览器，页面刷新后保留。所有请求会自动携带此身份 Header。
        </div>
      </Modal>
    </>
  )
}

export default ActorSettings
