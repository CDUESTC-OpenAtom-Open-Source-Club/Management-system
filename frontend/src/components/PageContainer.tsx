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
    <div className="app-page">
      <div className="page-container-head">
        <Title level={4} className="page-container-title">
          {title}
        </Title>
        {extra && <div className="page-container-extra">{extra}</div>}
      </div>
      {children}
    </div>
  )
}

export default PageContainer
