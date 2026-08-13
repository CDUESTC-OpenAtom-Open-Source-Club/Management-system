import React, { useEffect, useRef } from 'react'

interface Star {
  x: number
  y: number
  radius: number
  baseAlpha: number
  twinkleSpeed: number
  twinkleOffset: number
  depth: number
  driftX: number
  driftY: number
}

const random = (min: number, max: number) => Math.random() * (max - min) + min

const getStarCount = (width: number) => {
  if (width < 640) return 110
  if (width < 1280) return 180
  return 230
}

const createStars = (width: number, height: number): Star[] => {
  const count = getStarCount(width)
  const stars: Star[] = []

  for (let i = 0; i < count; i += 1) {
    const depth = Math.random()
    const isLargeStar = Math.random() < 0.08
    const radius = isLargeStar
      ? random(1.1, 1.8)
      : depth < 0.75
        ? random(0.4, 0.7)
        : random(0.7, 1.15)

    stars.push({
      x: Math.random() * width,
      y: Math.random() * height,
      radius,
      baseAlpha: isLargeStar ? random(0.42, 0.72) : random(0.22, 0.56),
      twinkleSpeed: random(0.00025, 0.0009),
      twinkleOffset: random(0, Math.PI * 2),
      depth,
      driftX: random(-0.12, 0.12) * depth,
      driftY: random(-0.08, 0.08) * depth,
    })
  }

  return stars
}

const LoginStarfield: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const starsRef = useRef<Star[]>([])
  const frameRef = useRef<number | null>(null)
  const lastTimeRef = useRef(0)
  const pointerRef = useRef({ x: 0, y: 0, active: false })
  const glowRef = useRef({ x: 0, y: 0, tx: 0, ty: 0 })

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let width = window.innerWidth
    let height = window.innerHeight
    let dpr = Math.min(window.devicePixelRatio || 1, 2)

    const resize = () => {
      width = window.innerWidth
      height = window.innerHeight
      dpr = Math.min(window.devicePixelRatio || 1, 2)
      canvas.width = Math.floor(width * dpr)
      canvas.height = Math.floor(height * dpr)
      canvas.style.width = `${width}px`
      canvas.style.height = `${height}px`
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      starsRef.current = createStars(width, height)
    }

    const handlePointerMove = (event: PointerEvent) => {
      pointerRef.current = { x: event.clientX, y: event.clientY, active: true }
      glowRef.current.tx = event.clientX
      glowRef.current.ty = event.clientY
    }

    const handlePointerLeave = () => {
      pointerRef.current.active = false
    }

    const drawNebula = () => {
      const layers = [
        { x: 0.18, y: 0.22, r: 0.75, a: 0.08, c: '20, 33, 61' },
        { x: 0.78, y: 0.18, r: 0.62, a: 0.06, c: '17, 24, 39' },
        { x: 0.52, y: 0.78, r: 0.7, a: 0.05, c: '13, 42, 74' },
      ]

      for (const layer of layers) {
        const gradient = ctx.createRadialGradient(
          width * layer.x,
          height * layer.y,
          0,
          width * layer.x,
          height * layer.y,
          Math.max(width, height) * layer.r,
        )
        gradient.addColorStop(0, `rgba(${layer.c}, ${layer.a})`)
        gradient.addColorStop(0.45, `rgba(${layer.c}, ${layer.a * 0.38})`)
        gradient.addColorStop(1, 'rgba(5, 11, 24, 0)')
        ctx.fillStyle = gradient
        ctx.fillRect(0, 0, width, height)
      }
    }

    const drawGlow = () => {
      const glow = glowRef.current
      glow.x += (glow.tx - glow.x) * 0.08
      glow.y += (glow.ty - glow.y) * 0.08

      const gradient = ctx.createRadialGradient(glow.x, glow.y, 0, glow.x, glow.y, 180)
      gradient.addColorStop(0, 'rgba(96, 165, 250, 0.09)')
      gradient.addColorStop(0.35, 'rgba(59, 130, 246, 0.05)')
      gradient.addColorStop(1, 'rgba(59, 130, 246, 0)')
      ctx.fillStyle = gradient
      ctx.beginPath()
      ctx.arc(glow.x, glow.y, 180, 0, Math.PI * 2)
      ctx.fill()
    }

    const animate = (time: number) => {
      const delta = lastTimeRef.current ? Math.min(time - lastTimeRef.current, 34) : 16
      lastTimeRef.current = time
      ctx.clearRect(0, 0, width, height)
      drawNebula()
      drawGlow()

      const pointer = pointerRef.current

      for (const star of starsRef.current) {
        const twinkle = 0.88 + Math.sin(time * star.twinkleSpeed + star.twinkleOffset) * 0.06 + Math.cos(time * star.twinkleSpeed * 1.7 + star.twinkleOffset) * 0.03
        const alpha = Math.max(0.16, Math.min(0.9, star.baseAlpha * twinkle))

        let x = star.x
        let y = star.y

        if (star.depth < 0.18) {
          x += star.driftX * (delta / 1000)
          y += star.driftY * (delta / 1000)
        }

        if (pointer.active) {
          const dx = x - pointer.x
          const dy = y - pointer.y
          const dist = Math.hypot(dx, dy)
          if (dist < 72) {
            const fade = 1 - dist / 72
            const boosted = Math.min(1, alpha + fade * 0.05)
            ctx.fillStyle = `rgba(210, 226, 255, ${boosted})`
          } else {
            ctx.fillStyle = `rgba(244, 248, 255, ${alpha})`
          }
        } else {
          ctx.fillStyle = `rgba(244, 248, 255, ${alpha})`
        }

        ctx.beginPath()
        ctx.arc(x, y, star.radius, 0, Math.PI * 2)
        ctx.fill()

        if (star.radius > 1.15) {
          ctx.fillStyle = `rgba(148, 179, 255, ${alpha * 0.08})`
          ctx.beginPath()
          ctx.arc(x, y, star.radius * 3.2, 0, Math.PI * 2)
          ctx.fill()
        }
      }

      frameRef.current = requestAnimationFrame(animate)
    }

    resize()
    window.addEventListener('resize', resize)
    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerleave', handlePointerLeave)
    frameRef.current = requestAnimationFrame(animate)

    return () => {
      window.removeEventListener('resize', resize)
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerleave', handlePointerLeave)
      if (frameRef.current) cancelAnimationFrame(frameRef.current)
    }
  }, [])

  return <canvas ref={canvasRef} className="login-particles" aria-hidden="true" />
}

export default LoginStarfield
