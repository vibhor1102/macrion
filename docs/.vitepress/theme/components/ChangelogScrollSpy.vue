<script setup lang="ts">
import { onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vitepress'

const route = useRoute()

let onScroll: (() => void) | null = null
let currentActiveId = ''

function updateActiveSidebarItem(activeId: string) {
  if (!activeId) return
  currentActiveId = activeId

  const sidebar = document.querySelector('.VPSidebar')
  if (!sidebar) return

  // Target only navigation link items (level-1+), never group headings like 'Versions' (level-0)
  const items = sidebar.querySelectorAll('.VPSidebarItem.level-1, .VPSidebarItem.is-link')
  items.forEach(item => {
    const link = item.querySelector(':scope > .item a, a') as HTMLAnchorElement | null
    if (!link) return

    const href = link.getAttribute('href') || ''
    const hash = href.includes('#') ? href.split('#')[1] : ''

    if (hash === activeId) {
      item.classList.add('is-active')
      // Ensure the active sidebar item is scrolled into view in long sidebars
      item.scrollIntoView?.({ block: 'nearest', inline: 'nearest', behavior: 'smooth' })
    } else {
      item.classList.remove('is-active')
    }
  })

  // Ensure level-0 group headers (e.g. 'Versions') never retain active state
  sidebar.querySelectorAll('.VPSidebarItem.level-0').forEach(header => {
    header.classList.remove('is-active')
  })
}

function getVersionHeadings(): HTMLElement[] {
  // Query all H2 elements with an id starting with 'v' (e.g. id="v0-3-0")
  const headings = Array.from(document.querySelectorAll('.vp-doc h2[id]')) as HTMLElement[]
  return headings
    .filter(h => /^v\d/.test(h.id))
    .sort((a, b) => a.offsetTop - b.offsetTop)
}

function getActiveHeading(headings: HTMLElement[]): HTMLElement | null {
  if (headings.length === 0) return null

  // If scrolled to near the bottom of the page, activate the last version
  const scrollBottom = window.innerHeight + window.scrollY
  const documentHeight = document.documentElement.scrollHeight
  if (scrollBottom >= documentHeight - 60) {
    return headings[headings.length - 1]
  }

  // Threshold: 110px from viewport top (accounts for fixed navbar and margin)
  const threshold = 110
  let active = headings[0]

  for (const heading of headings) {
    const rect = heading.getBoundingClientRect()
    if (rect.top <= threshold) {
      active = heading
    } else {
      break
    }
  }

  return active
}

function scrollToHeading(hash: string, smooth: boolean = true) {
  const el = document.getElementById(hash)
  if (!el) return

  const navHeight = 64
  const offset = 28
  const top = el.getBoundingClientRect().top + window.scrollY - (navHeight + offset)

  window.scrollTo({
    top: Math.max(0, top),
    behavior: smooth ? 'smooth' : 'auto'
  })
}

function onSidebarClick(event: Event) {
  const target = (event.target as HTMLElement).closest('a') as HTMLAnchorElement | null
  if (!target) return

  const href = target.getAttribute('href') || ''
  if (href.includes('#')) {
    const hash = href.split('#')[1]
    if (hash) {
      event.preventDefault()
      updateActiveSidebarItem(hash)
      scrollToHeading(hash, true)

      if (window.history.pushState) {
        window.history.pushState(null, '', `#${hash}`)
      }

      // Close mobile drawer if open
      if (window.innerWidth < 960) {
        const backdrop = document.querySelector('.VPBackdrop') as HTMLElement | null
        backdrop?.click()
      }
    }
  }
}

function initScrollSpy() {
  cleanup()

  // Only run on the changelog page
  if (!route.path.includes('/changelog')) return

  const headings = getVersionHeadings()
  if (headings.length === 0) return

  // Check if URL already has a hash
  const initialHash = window.location.hash.replace(/^#/, '')
  const matchingInitial = headings.find(h => h.id === initialHash)

  if (matchingInitial) {
    updateActiveSidebarItem(matchingInitial.id)
    setTimeout(() => {
      scrollToHeading(matchingInitial.id, false)
    }, 100)
  } else {
    const active = getActiveHeading(headings)
    if (active) updateActiveSidebarItem(active.id)
  }

  // Listen for sidebar clicks for immediate visual response
  const sidebar = document.querySelector('.VPSidebar')
  if (sidebar) {
    sidebar.addEventListener('click', onSidebarClick)
  }

  // Throttled scroll listener
  let ticking = false
  onScroll = () => {
    if (!ticking) {
      window.requestAnimationFrame(() => {
        const active = getActiveHeading(headings)
        if (active && active.id !== currentActiveId) {
          updateActiveSidebarItem(active.id)
          if (window.history.replaceState) {
            window.history.replaceState(null, '', `#${active.id}`)
          }
        }
        ticking = false
      })
      ticking = true
    }
  }

  window.addEventListener('scroll', onScroll, { passive: true })
}

function cleanup() {
  if (onScroll) {
    window.removeEventListener('scroll', onScroll)
    onScroll = null
  }
  const sidebar = document.querySelector('.VPSidebar')
  if (sidebar) {
    sidebar.removeEventListener('click', onSidebarClick)
  }
}

watch(() => route.path, () => {
  nextTick(() => {
    setTimeout(initScrollSpy, 150)
  })
})

onMounted(() => {
  nextTick(() => {
    setTimeout(initScrollSpy, 150)
  })
})

onUnmounted(() => {
  cleanup()
})
</script>

<template>
  <span class="changelog-scroll-spy-mount" style="display: none;" aria-hidden="true"></span>
</template>
