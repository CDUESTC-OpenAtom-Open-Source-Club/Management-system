import React, { useEffect, useState, useCallback } from 'react'
import {
  Card, Select, Checkbox, Button, Table, Tag, Space,
  Divider, message, Typography, Alert,
} from 'antd'
import { SendOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import PageContainer from '../components/PageContainer'
import { getMembers } from '../api/member'
import {
  getPointApplyOptions,
  submitPointApplications,
  getMyPointApplications,
} from '../api/point'
import type { Member } from '../types/member'
import type { PointItem, PointApplication } from '../types/point'

const { Text } = Typography

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'gold', label: '待审核' },
  APPROVED: { color: 'green', label: '已通过' },
  REJECTED: { color: 'red', label: '已驳回' },
}

const MyApplications: React.FC = () => {
  const [members, setMembers] = useState<Member[]>([])
  const [selectedMemberId, setSelectedMemberId] = useState<number | undefined>()
  const [applyOptions, setApplyOptions] = useState<PointItem[]>([])
  const [checkedIds, setCheckedIds] = useState<number[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [myRecords, setMyRecords] = useState<PointApplication[]>([])
  const [loadingRecords, setLoadingRecords] = useState(false)

  // 加载所有成员（用于 Select）
  useEffect(() => {
    getMembers({ page: 1, size: 1000 }).then((res) => {
      setMembers(res.list ?? [])
    })
    getPointApplyOptions().then(setApplyOptions)
  }, [])

  // 加载我的登记记录
  const fetchMyRecords = useCallback(async () => {
    if (!selectedMemberId) return
    setLoadingRecords(true)
    try {
      const records = await getMyPointApplications(selectedMemberId)
      setMyRecords(records ?? [])
    } finally {
      setLoadingRecords(false)
    }
  }, [selectedMemberId])

  useEffect(() => { fetchMyRecords() }, [fetchMyRecords])

  const handleSubmit = async () => {
    if (!selectedMemberId) {
      message.warning('请先选择成员')
      return
    }
    if (checkedIds.length === 0) {
      message.warning('请至少选择一个积分项目')
      return
    }
    setSubmitting(true)
    try {
      await submitPointApplications({ memberId: selectedMemberId, pointItemIds: checkedIds })
      message.success('登记提交成功，等待审核')
      setCheckedIds([])
      fetchMyRecords()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<PointApplication> = [
    { title: '积分项目', dataIndex: 'itemName', width: 140 },
    {
      title: '分值', dataIndex: 'pointValue', width: 80,
      render: (v?: number) => v !== undefined ? (
        <Tag color={v > 0 ? 'green' : 'red'}>{v > 0 ? `+${v}` : v}</Tag>
      ) : '—'
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (s: string) => {
        const t = STATUS_TAG[s]
        return t ? <Tag color={t.color}>{t.label}</Tag> : s
      }
    },
    {
      title: '提交时间', dataIndex: 'createdAt', width: 160,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'
    },
    {
      title: '审核时间', dataIndex: 'reviewedAt', width: 160,
      render: (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—'
    },
    { title: '审核人', dataIndex: 'reviewedBy', width: 90 },
    { title: '审核意见', dataIndex: 'reviewComment', ellipsis: true },
  ]

  return (
    <PageContainer title="我的活动登记">
      {/* 选择成员 */}
      <Card style={{ marginBottom: 16 }}>
        <Space align="center">
          <Text strong>选择当前成员：</Text>
          <Select
            showSearch
            placeholder="请选择成员（姓名/学号）"
            style={{ width: 280 }}
            value={selectedMemberId}
            onChange={(v) => { setSelectedMemberId(v); setCheckedIds([]) }}
            filterOption={(input, option) =>
              String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
            options={members.map((m) => ({
              label: `${m.name}（${m.studentNo}）`,
              value: m.id,
            }))}
          />
        </Space>
      </Card>

      {selectedMemberId ? (
        <>
          {/* 登记表单 */}
          <Card
            title="我参加了："
            style={{ marginBottom: 16 }}
            extra={
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleSubmit}
                loading={submitting}
                disabled={checkedIds.length === 0}
              >
                提交登记
              </Button>
            }
          >
            {applyOptions.length === 0 ? (
              <Alert message="暂无可登记的积分项目" type="info" showIcon />
            ) : (
              <Checkbox.Group
                value={checkedIds}
                onChange={(vals) => setCheckedIds(vals as number[])}
              >
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  {applyOptions.map((item) => (
                    <Checkbox key={item.id} value={item.id}>
                      <Space>
                        <Text>{item.itemName}</Text>
                        <Tag color="blue">+{item.pointValue} 分</Tag>
                        {item.description && (
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {item.description}
                          </Text>
                        )}
                      </Space>
                    </Checkbox>
                  ))}
                </Space>
              </Checkbox.Group>
            )}
          </Card>

          <Divider>我的登记记录</Divider>

          {/* 登记记录 */}
          <Table
            rowKey="id"
            columns={columns}
            dataSource={myRecords}
            loading={loadingRecords}
            scroll={{ x: 800 }}
            pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 条` }}
          />
        </>
      ) : (
        <Alert
          message="请先在上方选择一个成员，再进行活动登记"
          type="info"
          showIcon
        />
      )}
    </PageContainer>
  )
}

export default MyApplications
