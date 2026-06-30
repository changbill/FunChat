const RefreshIcon = () => (
  <svg
    className="room-list__refresh-svg"
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden
  >
    <path
      d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M3 3v5h5"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M16 21h5v-5"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
)

/**
 * 스타일은 RoomList와 함께 쓸 때 `RoomList.css`의 `.room-list__refresh` 계열을 사용한다.
 */
const RefreshButton = ({
  onClick,
  disabled = false,
  spinning = false,
  ariaLabel = '목록 새로고침',
  title = '새로고침',
}) => {
  return (
    <button
      type="button"
      className={`room-list__refresh${spinning ? ' room-list__refresh--spinning' : ''}`}
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
      title={title}
    >
      <RefreshIcon />
    </button>
  )
}

export default RefreshButton
