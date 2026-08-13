import React, { useEffect, useRef } from 'react'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  life: number
  maxLife: number
  color: string
  drift: number
}

const MAX_PARTICLES = 80
const COLORS = [
  '255,255,255,0.85',
  '191,219,254,0.75',
  '96,165,250,0.65',
  '59,130,246,0.55',
]

const random = (min: number, max: number) => Math.random() * (max - min) + min

const LoginParticles: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const particlesRef = useRef<Particle[]>([])
  const frameRef = useRef<number | null>(null)
  const lastTimeRef = useRef(0)
  const pointerRef = useRef({ x: 0, y: 0, active: false, speed: 0 })

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduceMotion) return

    let dpr = 1
    const resize = () => {
      dpr = Math.min(window.devicePixelRatio || 1, 2)
      canvas.width = Math.floor(window.innerWidth * dpr)
      canvas.height = Math.floor(window.innerHeight * dpr)
      canvas.style.width = `${window.innerWidth}px`
      canvas.style.height = `${window.innerHeight}px`
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }

    const spawnParticles = (x: number, y: number, speed: number) => {
      const count = Math.min(7, Math.max(4, Math.round(4 + speed / 18)))
      for (let i = 0; i < count; i += 1) {
        if (particlesRef.current.length >= MAX_PARTICLES) {
          particlesRef.current.shift()
        }

        const angle = random(0, Math.PI * 2)
        const spread = random(0.2, 1.5) + speed * 0.01
        const life = random(1000, 1800)
        particlesRef.current.push({
          x: x + random(-10, 10),
          y: y + random(-10, 10),
          vx: Math.cos(angle) * spread + random(-0.35, 0.35),
          vy: Math.sin(angle) * spread - random(0.2, 0.8),
          size: random(2, 7),
          life,
          maxLife: life,
          color: COLORS[Math.floor(Math.random() * COLORS.length)],
          drift: random(0.25, 0.9),
        })
      }
    }

    const handlePointerMove = (event: PointerEvent) => {
      const previousX = pointerRef.current.x || event.clientX
      const previousY = pointerRef.current.y || event.clientY
      const dx = event.clientX - previousX
      const dy = event.clientY - previousY
      const speed = Math.min(Math.hypot(dx, dy), 90)

      pointerRef.current = {
        x: event.clientX,
        y: event.clientY,
        active: true,
        speed,
      }

      spawnParticles(event.clientX, event.clientY, speed)
    }

    const handlePointerLeave = () => {
      pointerRef.current.active = false
    }

    const drawParticle = (particle: Particle) => {
      const progress = Math.max(particle.life / particle.maxLife, 0)
      const alpha = progress
      const radius = particle.size * (0.8 + (1 - progress) * 0.35)
      const [r, g, b, baseAlpha] = particle.color.split(',').map(Number)
      const gradient = ctx.createRadialGradient(particle.x, particle.y, 0, particle.x, particle.y, radius * 2.8)
      gradient.addColorStop(0, `rgba(${r}, ${g}, ${b}, ${baseAlpha * alpha})`)
      gradient.addColorStop(0.4, `rgba(${r}, ${g}, ${b}, ${baseAlpha * alpha * 0.45})`)
      gradient.addColorStop(1, `rgba(${r}, ${g}, ${b}, 0)`)

      ctx.fillStyle = gradient
      ctx.beginPath()
      ctx.arc(particle.x, particle.y, radius * 2.8, 0, Math.PI * 2)
      ctx.fill()
    }

    const animate = (time: number) => {
      const delta = lastTimeRef.current ? Math.min(time - lastTimeRef.current, 32) : 16
      lastTimeRef.current = time
      ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)

      particlesRef.current = particlesRef.current.filter((particle) => {
        particle.x += particle.vx * delta * 0.08
        particle.y += particle.vy * delta * 0.08 - particle.drift * delta * 0.03
        particle.vx *= 0.992
        particle.vy *= 0.992
        particle.life -= delta

        if (particle.life <= 0) return false
        drawParticle(particle)
        return true
      })

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
      particlesRef.current = []
    }
  }, [])

  return <canvas className="login-particles" ref={canvasRef} aria-hidden="true" />
}

export default LoginParticles
