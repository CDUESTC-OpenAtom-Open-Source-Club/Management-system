import React from 'react'
import { Typography } from 'antd'

const { Title } = Typography

interface Props {
  title: string
  extra?: React.ReactNode
  children: React.ReactNode
}

const PageContainer: React.FC<Props> = ({ title, extra, children }) => {
  return (
    <div style={{ padding: '0 4px' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Title level={4} style={{ margin: 0 }}>
          {title}
        </Title>
        {extra && <div>{extra}</div>}
      </div>
      {children}
    </div>
  )
}

export default PageContainer
