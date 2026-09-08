import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import ChangelogScrollSpy from './components/ChangelogScrollSpy.vue'
import HomeLanding from './components/HomeLanding.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('HomeLanding', HomeLanding)
  },
  Layout() {
    return h(DefaultTheme.Layout, null, {
      'layout-bottom': () => h(ChangelogScrollSpy)
    })
  }
}
