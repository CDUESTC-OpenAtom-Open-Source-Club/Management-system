import React from 'react'
import { RouterProvider } from 'react-router-dom'
import { App as AntdApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import router from './router'

const App: React.FC = () => {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#2f6bff',
          colorBgLayout: '#f6f8fb',
          colorBorderSecondary: '#eaecf0',
          colorText: '#101828',
          colorTextSecondary: '#475467',
          borderRadius: 10,
          fontSize: 14,
        },
      }}
    >
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  )
}

export default App
