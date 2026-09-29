<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { SliderCaptcha as SliderCaptchaModel } from '@/api/auth'

const props = defineProps<{
  /** 后端签发的滑块验证码数据；null 表示尚未加载。 */
  model: SliderCaptchaModel | null
  /** 是否正在校验（锁交互）。 */
  verifying: boolean
  /** 验证成功（出 ✓，轨道变绿，拼图块滑入缺口）。 */
  success: boolean
  /** 校验失败（短暂显示后自动换新图）。 */
  failed: boolean
}>()

const emit = defineEmits<{
  /** 拖拽完成：图片坐标系下的偏移 px、轨迹 JSON、用时 ms。 */
  verified: [offset: number, trajectory: string, durationMs: number]
}>()

const THUMB = 36 // 滑块厚度 px，与 CSS 保持一致

const trackRef = ref<HTMLElement | null>(null)

const dragging = ref(false)
/** 拖拽进度 0..1（对应拼图块在图片坐标内的 travel 占比）。 */
const progress = ref(0)
const dragStart = ref(0)
const dragStartTs = ref(0)
/** 采样轨迹：{ x: 进度, t: 相对 ms }。 */
const samples = ref<{ x: number; t: number }[]>([])

/** 轨道渲染宽度（px），ResizeObserver 更新。 */
const trackRenderWidth = ref(0)

let lastSampleTs = 0
let ro: ResizeObserver | null = null

onMounted(() => {
  const el = trackRef.value
  if (el) {
    trackRenderWidth.value = el.clientWidth
    ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        trackRenderWidth.value = entry.contentRect.width
      }
    })
    ro.observe(el)
  }
})

onBeforeUnmount(() => {
  ro?.disconnect()
})

/** 滑块最大行程 = 轨道宽 - 滑块厚。 */
const thumbTravel = computed(() => Math.max(0, trackRenderWidth.value - THUMB))

/** 滑块位移（px）：按行程换算，而非自身宽度百分比（修复原 100% 只移动 34px 的问题）。 */
const thumbOffsetPx = computed(() => progress.value * thumbTravel.value)

function onPointerDown(e: PointerEvent) {
  if (props.verifying || props.success || props.failed) return
  const track = trackRef.value
  if (!track || trackRenderWidth.value <= 0) return
  const rect = track.getBoundingClientRect()
  dragStart.value = e.clientX - rect.left
  dragStartTs.value = performance.now()
  samples.value = []
  lastSampleTs = 0
  dragging.value = true
  ;(e.currentTarget as Element).setPointerCapture?.(e.pointerId)
  e.preventDefault()
}

function onPointerMove(e: PointerEvent) {
  if (!dragging.value || !props.model || thumbTravel.value <= 0) return
  const track = trackRef.value
  if (!track) return
  const rect = track.getBoundingClientRect()
  // 按行程（轨道宽 - 滑块宽）换算，progress=1 时滑块贴到右端
  const raw = (e.clientX - rect.left - dragStart.value - THUMB / 2) / thumbTravel.value
  progress.value = Math.max(0, Math.min(1, raw))

  const t = performance.now() - dragStartTs.value
  if (t - lastSampleTs >= 30) {
    samples.value.push({ x: progress.value, t })
    lastSampleTs = t
  }
}

function onPointerUp() {
  if (!dragging.value || !props.model || props.verifying) return
  dragging.value = false
  const t = performance.now() - dragStartTs.value
  samples.value.push({ x: progress.value, t })

  // 图片坐标系下的偏移：progress=1 → trackWidth（拼图块到达最右端）
  const imageOffset = progress.value * props.model.trackWidth
  const traj = JSON.stringify({
    durationMs: Math.round(t),
    points: samples.value
      .filter((_, i) => i % 2 === 0)
      .map((s) => [Math.round(s.x * 1000) / 1000, Math.round(s.t)] as [number, number]),
  })
  emit('verified', Math.round(imageOffset), traj, Math.round(t))
}

function nudge(delta: number) {
  if (props.verifying || props.success || props.failed) return
  const step = delta / Math.max(1, thumbTravel.value)
  progress.value = Math.max(0, Math.min(1, progress.value + step))
  samples.value.push({ x: progress.value, t: performance.now() })
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowRight') {
    e.preventDefault()
    nudge(2)
  } else if (e.key === 'ArrowLeft') {
    e.preventDefault()
    nudge(-2)
  } else if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    onPointerUp()
  }
}

watch(
  () => props.model?.challengeId,
  () => {
    progress.value = 0
    dragging.value = false
    samples.value = []
  },
)

</script>

<template>
  <div
    class="sc-root"
    :class="{ 'is-success': success, 'is-failed': failed, 'is-disabled': verifying || success || failed }"
  >
    <!-- 拼图区 -->
    <div class="sc-board" :class="{ 'is-failed': failed }" :style="{ aspectRatio: '2 / 1' }">
      <img v-if="model?.backgroundImage" :src="model.backgroundImage" alt="" class="sc-bg" />
      <img
        v-if="model?.pieceImage"
        :src="model.pieceImage"
        alt=""
        class="sc-piece"
        :class="{ 'is-dragging': dragging, 'is-success': success }"
        :style="{
          left: model ? `calc(${(progress * model.trackWidth) / model.imageWidth * 100}% )` : undefined,
          top: model ? `calc(${(model.pieceY / model.imageHeight) * 100}% )` : undefined,
          width: model ? `calc(${(model.pieceSize / model.imageWidth) * 100}% )` : undefined,
          height: model ? `calc(${(model.pieceSize / model.imageHeight) * 100}% )` : undefined,
        }"
      />
      <div v-if="!model" class="sc-skeleton" aria-hidden="true">
        <span>加载中…</span>
      </div>
    </div>

    <!-- 轨道 -->
    <div class="sc-track-row">
      <div
        ref="trackRef"
        class="sc-track"
        :class="{ 'is-dragging': dragging }"
        tabindex="0"
        role="slider"
        :aria-label="model ? '拖动滑块，让拼图块与缺口对齐' : '验证码加载中'"
        :aria-valuemin="0"
        :aria-valuemax="100"
        :aria-valuenow="Math.round(progress * 100)"
        :aria-disabled="verifying || success || failed"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerUp"
        @keydown="onKeydown"
      >
        <div class="sc-track-fill" :style="{ width: `${thumbOffsetPx + THUMB}px` }" />
        <div
          class="sc-thumb"
          :class="{ 'is-dragging': dragging, 'is-success': success, 'is-failed': failed }"
          :style="{ transform: `translateX(${thumbOffsetPx}px)` }"
        >
          <span v-if="success" class="sc-thumb-check">✓</span>
          <span v-else class="sc-thumb-arrow">›</span>
        </div>
      </div>
    </div>

    <div class="sc-meta" aria-live="polite">
      <span v-if="success" class="sc-meta-ok">✓ 验证通过</span>
      <span v-else-if="verifying">校验中…</span>
      <span v-else-if="failed">未对齐，已刷新</span>
      <span v-else-if="progress === 0">向右拖动滑块，对齐缺口后松手</span>
      <span v-else>松手完成验证（可用 ← → 微调，回车确认）</span>
    </div>
  </div>
</template>

<style scoped>
.sc-root {
  width: 100%;
}

/* ---- 拼图区 ---- */
.sc-board {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: var(--wp-hairline);
  box-shadow: inset 0 0 0 1px var(--wp-mist);
  transition: box-shadow 160ms ease;
}
.sc-board.is-failed {
  animation: sc-board-shake 480ms ease;
  box-shadow: inset 0 0 0 2px var(--wp-brick);
}
.sc-bg {
  width: 100%;
  height: 100%;
  display: block;
}
.sc-piece {
  position: absolute;
  object-fit: contain;
  filter: drop-shadow(0 2px 4px rgba(16, 59, 58, 0.35));
  pointer-events: none;
}
.sc-piece.is-dragging {
  filter: drop-shadow(0 3px 7px rgba(16, 59, 58, 0.45));
}
.is-success .sc-piece {
  filter: drop-shadow(0 0 6px rgba(36, 128, 95, 0.7));
}

/* 加载骨架（shimmer） */
.sc-skeleton {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, var(--wp-hairline) 30%, #fff 50%, var(--wp-hairline) 70%);
  background-size: 200% 100%;
  animation: sc-shimmer 1.4s ease-in-out infinite;
  display: grid;
  place-items: center;
}
.sc-skeleton span {
  font-size: 12px;
  color: var(--wp-muted);
  background: var(--wp-paper);
  padding: 4px 10px;
  border-radius: 999px;
}

/* ---- 轨道 ---- */
.sc-track-row {
  margin-top: 12px;
}
.sc-track {
  position: relative;
  height: 36px;
  border-radius: 8px;
  background: var(--wp-hairline);
  box-shadow: inset 0 0 0 1px var(--wp-mist);
  touch-action: none;
  user-select: none;
  outline: none;
  transition: box-shadow 140ms ease;
}
.sc-track:focus-visible {
  box-shadow: inset 0 0 0 2px var(--wp-moss);
}
.sc-track.is-dragging {
  box-shadow: inset 0 0 0 1px var(--wp-mist), 0 0 0 3px rgba(46, 109, 103, 0.14);
}
.sc-track-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 8px 0 0 8px;
  background: linear-gradient(90deg, rgba(46, 109, 103, 0.16), rgba(46, 109, 103, 0.28));
  transition: background 160ms ease;
  pointer-events: none;
}
.is-success .sc-track-fill {
  background: linear-gradient(90deg, rgba(36, 128, 95, 0.18), rgba(36, 128, 95, 0.4));
}
.is-failed .sc-track-fill {
  background: linear-gradient(90deg, rgba(196, 71, 46, 0.14), rgba(196, 71, 46, 0.3));
}

/* ---- 滑块 ---- */
.sc-thumb {
  position: absolute;
  top: 0;
  left: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff 0%, #f4f8f6 100%);
  box-shadow:
    0 1px 2px rgba(16, 59, 58, 0.18),
    0 0 0 1px rgba(16, 59, 58, 0.06);
  display: grid;
  place-items: center;
  cursor: grab;
  color: var(--wp-muted);
  font-size: 18px;
  font-weight: 600;
  transition: background 140ms, box-shadow 140ms;
}
.sc-thumb.is-dragging {
  cursor: grabbing;
  background: linear-gradient(180deg, #fff 0%, #eef6f1 100%);
  box-shadow:
    0 3px 8px rgba(16, 59, 58, 0.22),
    0 0 0 1px rgba(46, 109, 103, 0.25);
  color: var(--wp-moss);
}
.sc-thumb.is-success {
  background: linear-gradient(180deg, #eaf6ef 0%, #d6efe3 100%);
  box-shadow: 0 0 0 2px rgba(36, 128, 95, 0.35);
  color: #24805f;
  font-weight: 800;
  animation: sc-pop 320ms cubic-bezier(0.2, 1.4, 0.5, 1);
}
.sc-thumb.is-failed {
  color: var(--wp-brick);
}
.sc-thumb-check {
  line-height: 1;
}

/* ---- 状态行 ---- */
.sc-meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--wp-muted);
  min-height: 16px;
  transition: color 160ms;
}
.sc-meta-ok {
  color: #24805f;
  font-weight: 600;
}
.is-disabled .sc-track {
  opacity: 0.85;
}
.is-disabled .sc-thumb {
  cursor: default;
}

/* ---- 动画 ---- */
@keyframes sc-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@keyframes sc-board-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(5px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(3px); }
}
@keyframes sc-pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.12); }
  100% { transform: scale(1); }
}

@media (prefers-reduced-motion: reduce) {
  .sc-board.is-failed,
  .sc-thumb.is-success,
  .sc-skeleton {
    animation: none;
  }
}
</style>
