import React, { useEffect, useRef } from 'react'

interface Particle {
  x: number
  y: number
  ox: number       // original x (home)
  oy: number       // original y (home)
  vx: number
  vy: number
  size: number
  alpha: number
  color: string    // css rgba string
  type: 'dot' | 'line'
  angle: number    // line angle
  length: number   // line length
}

const PALETTE = [
  '0, 240, 255',   // #00F0FF cyan
  '139, 92, 246',  // #8B5CF6 purple
  '59, 130, 246',  // #3B82F6 blue
  '148, 163, 184', // #94A3B8 slate (rare)
]

const random = (min: number, max: number) => Math.random() * (max - min) + min

const getCount = (w: number, h: number) => {
  const area = w * h
  if (area < 500_000) return 90
  if (area < 1_000_000) return 140
  return 200
}

const spawnParticles = (w: number, h: number): Particle[] => {
  const count = getCount(w, h)
  const particles: Particle[] = []
  for (let i = 0; i < count; i++) {
    const x = Math.random() * w
    const y = Math.random() * h
    const color = PALETTE[Math.floor(Math.random() * PALETTE.length)]
    const isLine = Math.random() < 0.45
    particles.push({
      x,
      y,
      ox: x,
      oy: y,
      vx: 0,
      vy: 0,
      size: isLine ? random(0.5, 1.2) : random(1.0, 2.4),
      alpha: random(0.25, 0.7),
      color,
      type: isLine ? 'line' : 'dot',
      angle: isLine ? random(0, Math.PI * 2) : 0,
      length: isLine ? random(4, 14) : 0,
    })
  }
  return particles
}

const AntigravityParticles: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const particlesRef = useRef<Particle[]>([])
  const mouseRef = useRef({ x: -9999, y: -9999, active: false })
  const frameRef = useRef<number>(0)
  const dimsRef = useRef({ w: 0, h: 0, dpr: 1 })

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      const w = window.innerWidth
      const h = window.innerHeight
      canvas.width = Math.floor(w * dpr)
      canvas.height = Math.floor(h * dpr)
      canvas.style.width = `${w}px`
      canvas.style.height = `${h}px`
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      dimsRef.current = { w, h, dpr }
      particlesRef.current = spawnParticles(w, h)
    }

    const onMove = (e: PointerEvent) => {
      mouseRef.current = { x: e.clientX, y: e.clientY, active: true }
    }
    const onLeave = () => {
      mouseRef.current.active = false
    }

    const REPEL_RADIUS = 160
    const REPEL_FORCE = 2.8
    const RETURN_SPEED = 0.04
    const FRICTION = 0.92

    const animate = () => {
      const { w, h } = dimsRef.current
      ctx.clearRect(0, 0, w, h)

      const mx = mouseRef.current.x
      const my = mouseRef.current.y
      const mouseActive = mouseRef.current.active

      for (const p of particlesRef.current) {
        // Anti-gravity: push away from mouse
        if (mouseActive) {
          const dx = p.x - mx
          const dy = p.y - my
          const dist = Math.hypot(dx, dy)
          if (dist < REPEL_RADIUS && dist > 0.1) {
            const force = (1 - dist / REPEL_RADIUS) * REPEL_FORCE
            p.vx += (dx / dist) * force
            p.vy += (dy / dist) * force
          }
        }

        // Return to original position
        p.vx += (p.ox - p.x) * RETURN_SPEED
        p.vy += (p.oy - p.y) * RETURN_SPEED

        // Apply friction + move
        p.vx *= FRICTION
        p.vy *= FRICTION
        p.x += p.vx
        p.y += p.vy

        // Soft clamp to viewport
        if (p.x < -40) p.x = -40
        if (p.x > w + 40) p.x = w + 40
        if (p.y < -40) p.y = -40
        if (p.y > h + 40) p.y = h + 40

        // Draw
        ctx.save()
        ctx.globalAlpha = p.alpha

        if (p.type === 'dot') {
          // Glow dot
          const grad = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.size * 2.5)
          grad.addColorStop(0, `rgba(${p.color}, ${p.alpha})`)
          grad.addColorStop(0.35, `rgba(${p.color}, ${p.alpha * 0.5})`)
          grad.addColorStop(1, `rgba(${p.color}, 0)`)
          ctx.fillStyle = grad
          ctx.beginPath()
          ctx.arc(p.x, p.y, p.size * 2.5, 0, Math.PI * 2)
          ctx.fill()
        } else {
          // Colored micro line
          ctx.strokeStyle = `rgba(${p.color}, ${p.alpha})`
          ctx.lineWidth = p.size
          ctx.lineCap = 'round'
          ctx.beginPath()
          const lx = Math.cos(p.angle) * p.length
          const ly = Math.sin(p.angle) * p.length
          ctx.moveTo(p.x - lx, p.y - ly)
          ctx.lineTo(p.x + lx, p.y + ly)
          ctx.stroke()
        }

        ctx.restore()
      }

      frameRef.current = requestAnimationFrame(animate)
    }

    resize()
    window.addEventListener('resize', resize)
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerleave', onLeave)
    frameRef.current = requestAnimationFrame(animate)

    return () => {
      window.removeEventListener('resize', resize)
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerleave', onLeave)
      cancelAnimationFrame(frameRef.current)
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      className="antigravity-canvas"
      aria-hidden="true"
    />
  )
}

export default AntigravityParticles
