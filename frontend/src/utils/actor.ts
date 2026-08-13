const ACTOR_KEY = 'club_actor'

export interface ActorInfo {
  name: string
  department: string
  position: string
}

const DEFAULT_ACTOR: ActorInfo = {
  name: '张三',
  department: '秘书处',
  position: '会长',
}

export function getActor(): ActorInfo {
  try {
    const raw = localStorage.getItem(ACTOR_KEY)
    if (raw) return JSON.parse(raw) as ActorInfo
  } catch {
    // ignore
  }
  return { ...DEFAULT_ACTOR }
}

export function setActor(actor: ActorInfo): void {
  localStorage.setItem(ACTOR_KEY, JSON.stringify(actor))
}

export function clearActor(): void {
  localStorage.removeItem(ACTOR_KEY)
}
