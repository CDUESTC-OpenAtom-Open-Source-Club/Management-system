import React, { useEffect, useMemo, useState } from 'react'
import { Modal, Input, Empty } from 'antd'
import { SearchOutlined, EnterOutlined } from '@ant-design/icons'
import { visibleNavItems, NavItem } from '../config/navigation'

interface CommandPaletteProps {
  open: boolean
  onClose: () => void
  onNavigate: (item: NavItem) => void
}

const CommandPalette: React.FC<CommandPaletteProps> = ({ open, onClose, onNavigate }) => {
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)

  const items = useMemo(() => {
    const all = visibleNavItems()
    const q = query.trim().toLowerCase()
    if (!q) return all
    return all.filter((i) => i.label.toLowerCase().includes(q))
  }, [query])

  useEffect(() => {
    if (open) {
      setQuery('')
      setActiveIndex(0)
    }
  }, [open])

  useEffect(() => {
    setActiveIndex(0)
  }, [query])

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActiveIndex((i) => Math.min(i + 1, items.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIndex((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      const item = items[activeIndex]
      if (item) onNavigate(item)
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      closable={false}
      width={520}
      destroyOnClose
      centered
      className="command-palette"
      styles={{ body: { padding: 0 } }}
    >
      <div className="cp-search">
        <SearchOutlined className="cp-search-icon" />
        <Input
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="搜索功能或页面…"
          bordered={false}
          className="cp-search-input"
        />
        <span className="cp-search-esc">ESC</span>
      </div>
      <div className="cp-list">
        {items.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配的功能" style={{ padding: '28px 0' }} />
        ) : (
          items.map((item, index) => (
            <div
              key={item.key}
              className={`cp-item${index === activeIndex ? ' active' : ''}`}
              role="button"
              tabIndex={0}
              onMouseEnter={() => setActiveIndex(index)}
              onClick={() => onNavigate(item)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  onNavigate(item)
                }
              }}
            >
              <span className="cp-item-icon">{item.icon}</span>
              <span className="cp-item-label">{item.label}</span>
              <EnterOutlined className="cp-item-enter" />
            </div>
          ))
        )}
      </div>
    </Modal>
  )
}

export default CommandPalette
