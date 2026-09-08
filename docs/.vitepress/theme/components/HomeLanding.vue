<script setup lang="ts">
const downloadUrl = 'https://github.com/vibhor1102/macrion/releases/latest'
</script>

<template>
  <main class="macrion-home">
    <section class="macrion-hero" aria-labelledby="macrion-hero-title">
      <div class="macrion-hero-copy">
        <p class="macrion-eyebrow">A visual automation engine for Android</p>
        <h1 id="macrion-hero-title">When this appears,<br><span>do that.</span></h1>
        <p class="macrion-lede">
          Build Android automations that respond to images, text, colors, timers,
          and device events—then tap, swipe, type, or trigger another tool.
        </p>

        <div class="macrion-actions">
          <a class="macrion-button macrion-button-primary" :href="downloadUrl">
            Download Macrion
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v12m0 0 5-5m-5 5-5-5M5 21h14" />
            </svg>
          </a>
          <a class="macrion-button macrion-button-secondary" href="/macrion/guides/getting-started/quick-start">
            Read the quick start
          </a>
        </div>

        <p class="macrion-trust">Free and open source <span aria-hidden="true">·</span> No analytics <span aria-hidden="true">·</span> Runs on Android</p>
      </div>

      <div class="macrion-flow-wrap" aria-label="Example Macrion automation">
        <div class="macrion-flow-glow" aria-hidden="true" />
        <div class="macrion-flow">
          <div class="macrion-flow-header">
            <span class="macrion-flow-dot" />
            <div>
              <strong>Claim reward</strong>
              <span>Smart scenario</span>
            </div>
            <span class="macrion-flow-status">Ready</span>
          </div>

          <div class="macrion-node macrion-node-when">
            <span class="macrion-node-type">When</span>
            <span>“Continue” appears</span>
            <i class="macrion-node-icon" aria-hidden="true">⌗</i>
          </div>
          <div class="macrion-connector" aria-hidden="true" />
          <div class="macrion-node">
            <span class="macrion-node-type">And</span>
            <span>Counter is below 10</span>
            <i class="macrion-node-icon" aria-hidden="true">&lt;</i>
          </div>
          <div class="macrion-connector" aria-hidden="true" />
          <div class="macrion-node macrion-node-do">
            <span class="macrion-node-type">Do</span>
            <span>Tap detected position</span>
            <i class="macrion-node-icon" aria-hidden="true">◎</i>
          </div>
          <div class="macrion-connector" aria-hidden="true" />
          <div class="macrion-node">
            <span class="macrion-node-type">Then</span>
            <span>Increment counter</span>
            <i class="macrion-node-icon" aria-hidden="true">+1</i>
          </div>
        </div>
      </div>
    </section>

    <section class="macrion-capabilities" aria-label="What Macrion can do">
      <article>
        <span class="macrion-capability-number">01</span>
        <h2>Recognize what’s on screen</h2>
        <p>Match images, colors, text, and numbers without relying on fixed coordinates alone.</p>
      </article>
      <article>
        <span class="macrion-capability-number">02</span>
        <h2>Act and adapt</h2>
        <p>Tap, swipe, enter text, use counters, and control which events run next.</p>
      </article>
      <article>
        <span class="macrion-capability-number">03</span>
        <h2>Connect your tools</h2>
        <p>Work with Tasker, MacroDroid, broadcast intents, Quick Settings, and ADB.</p>
      </article>
    </section>

    <section class="macrion-closing" aria-labelledby="macrion-closing-title">
      <div>
        <p class="macrion-eyebrow">Local by design</p>
        <h2 id="macrion-closing-title">Automation that stays on your device.</h2>
        <p>No account, no analytics, and no screen data sent away.</p>
      </div>
      <a class="macrion-button macrion-button-primary" :href="downloadUrl">Get the latest release</a>
    </section>
  </main>
</template>
