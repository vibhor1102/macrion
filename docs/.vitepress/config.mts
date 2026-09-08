import { defineConfig } from 'vitepress'
import fs from 'node:fs'
import path from 'node:path'

function getChangelogSidebar() {
  const releasesDir = path.resolve(__dirname, '../changelog/releases')
  if (!fs.existsSync(releasesDir)) return []
  const files = fs.readdirSync(releasesDir)
    .filter(f => f.endsWith('.md') && f.startsWith('v'))

  files.sort((fileA, fileB) => {
    const parse = (f: string) => {
      const match = f.match(/^v?(\d+)\.(\d+)\.(\d+)(?:[-.]([0-9A-Za-z.-]+))?\.md$/)
      if (!match) return [0, 0, 0, '']
      return [parseInt(match[1], 10), parseInt(match[2], 10), parseInt(match[3], 10), match[4] || '']
    }
    const [majA, minA, patchA, preA] = parse(fileA)
    const [majB, minB, patchB, preB] = parse(fileB)
    if (majA !== majB) return (majB as number) - (majA as number)
    if (minA !== minB) return (minB as number) - (minA as number)
    if (patchA !== patchB) return (patchB as number) - (patchA as number)
    if (!preA && preB) return -1
    if (preA && !preB) return 1
    return fileB.localeCompare(fileA)
  })

  return [
    {
      text: 'Versions',
      items: files.map(file => {
        const version = file.replace(/\.md$/, '')
        const anchor = version.replace(/\./g, '-')
        return {
          text: version,
          link: `/changelog/#${anchor}`
        }
      })
    }
  ]
}

export default defineConfig({
  title: 'Macrion',
  description: 'Clean, image-aware Android automation',
  base: '/macrion/',
  srcExclude: ['changelog/releases/**'],
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/macrion/logo.svg' }],
    ['meta', { name: 'theme-color', content: '#000000' }],
    ['meta', { name: 'og:title', content: 'Macrion - Android Automation' }],
    ['meta', { name: 'og:description', content: 'Clean, open-source, image-aware Android automation app.' }],
    ['meta', { name: 'og:image', content: '/macrion/logo.svg' }]
  ],
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'Macrion',
    nav: [
      { text: 'Guides', link: '/guides/getting-started/quick-start' },
      { text: 'Documentation', link: '/documentation/' },
      { text: 'Changelog', link: '/changelog/' }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/vibhor1102/Macrion' }
    ],
    search: {
      provider: 'local'
    },
    outline: 'deep',
    sidebar: {
      '/guides/': [
        {
          text: 'Getting Started',
          collapsed: false,
          items: [
            { text: 'Quick Start Guide', link: '/guides/getting-started/quick-start' }
          ]
        }
      ],
      '/documentation/': [
        {
          text: 'Overview',
          collapsed: false,
          items: [
            { text: 'Documentation Index', link: '/documentation/' }
          ]
        },
        {
          text: 'App Settings & Safety',
          collapsed: false,
          items: [
            { text: 'Settings Catalog', link: '/documentation/settings/overview' },
            { text: 'Screen Capture & Display', link: '/documentation/settings/screen-capture' },
            { text: 'Device Workarounds & Safety', link: '/documentation/settings/input-safety' },
            { text: 'Diagnostics & Privacy', link: '/documentation/settings/diagnostics' }
          ]
        },
        {
          text: 'Vision & Screen Conditions',
          collapsed: false,
          items: [
            { text: 'Vision Engine Overview', link: '/documentation/conditions/' },
            { text: 'Image Conditions', link: '/documentation/conditions/condition-image' },
            { text: 'Color Conditions', link: '/documentation/conditions/condition-color' },
            { text: 'Text Conditions (OCR)', link: '/documentation/conditions/condition-text' },
            { text: 'Number Conditions', link: '/documentation/conditions/condition-number' },
            { text: 'Resolution & Scaling', link: '/documentation/conditions/resolution-scaling' }
          ]
        },
        {
          text: 'Event Triggers (Screen-Independent)',
          collapsed: false,
          items: [
            { text: 'Triggers Overview', link: '/documentation/triggers/' },
            { text: 'Timer Triggers', link: '/documentation/triggers/trigger-timer' },
            { text: 'Counter Triggers', link: '/documentation/triggers/trigger-counter' },
            { text: 'Broadcast Intent Triggers', link: '/documentation/triggers/trigger-broadcast' }
          ]
        },
        {
          text: 'Event Architecture & Logic',
          collapsed: false,
          items: [
            { text: 'Event Architecture', link: '/documentation/events/' },
            { text: 'Priority & Frame Flow', link: '/documentation/events/priority-and-frame-flow' },
            { text: 'Event Cooldowns', link: '/documentation/events/cooldowns' },
            { text: 'State Machines & Flow Control', link: '/documentation/events/dynamic-flow-control' }
          ]
        },
        {
          text: 'Actions & Gestures',
          collapsed: false,
          items: [
            { text: 'Actions Overview', link: '/documentation/actions/' },
            { text: 'Touch: Clicks & Swipes', link: '/documentation/actions/action-touch' },
            { text: 'Text Injection & Counters', link: '/documentation/actions/action-text' },
            { text: 'System Actions', link: '/documentation/actions/action-system' },
            { text: 'State & Flow Actions', link: '/documentation/actions/action-state' },
            { text: 'External Integration & Intents', link: '/documentation/actions/action-external' }
          ]
        },
        {
          text: 'Integration & Advanced Control',
          collapsed: false,
          items: [
            { text: 'Permissions & Security', link: '/documentation/advanced/permissions' },
            { text: 'Tasker, QS Tile & ADB', link: '/documentation/advanced/external-triggers' },
            { text: 'Performance Tuning', link: '/documentation/advanced/performance-tuning' },
            { text: 'Live Debugger & Profiling', link: '/documentation/advanced/debug-panel' }
          ]
        },
        {
          text: 'Data & Migration',
          collapsed: false,
          items: [
            { text: 'Backups & Storage', link: '/documentation/migration/backups' },
            { text: 'Klick\'r Migration & Compatibility', link: '/documentation/migration/klickr-migration' }
          ]
        }
      ],
      '/changelog/': getChangelogSidebar()
    },
    footer: {
      message: 'Free and open source under the GNU GPL v3.0 License.',
      copyright: 'Macrion Project'
    }
  }
})
