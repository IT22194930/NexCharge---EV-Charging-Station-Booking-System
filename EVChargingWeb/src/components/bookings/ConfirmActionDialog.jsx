import React from "react";

export default function ConfirmActionDialog({
  visible,
  icon = null,
  tone = "blue",
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  onConfirm,
  onCancel,
  onBackdrop,
}) {
  if (!visible) return null;

  const toneStyles = {
    blue: {
      ring: "bg-blue-100 text-blue-600",
      button: "bg-blue-600 hover:bg-blue-700",
    },
    green: {
      ring: "bg-green-100 text-green-600",
      button: "bg-green-600 hover:bg-green-700",
    },
    orange: {
      ring: "bg-orange-100 text-orange-600",
      button: "bg-orange-500 hover:bg-orange-600",
    },
    red: {
      ring: "bg-red-100 text-red-600",
      button: "bg-red-600 hover:bg-red-700",
    },
    emerald: {
      ring: "bg-emerald-100 text-emerald-600",
      button: "bg-emerald-600 hover:bg-emerald-700",
    },
  };

  const t = toneStyles[tone] || toneStyles.blue;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={onBackdrop || onCancel}
    >
      <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4" onClick={(e) => e.stopPropagation()}>
        <div className={`flex items-center justify-center w-12 h-12 mx-auto rounded-full ${t.ring}`}>
          {icon || (
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M13 16h-1v-4h-1m1-4h.01M12 2a10 10 0 100 20 10 10 0 000-20z"
              ></path>
            </svg>
          )}
        </div>
        <div className="mt-3 text-center">
          <h3 className="text-lg font-medium text-gray-900">{title}</h3>
          <div className="mt-2">
            <p className="text-sm text-gray-500">{message}</p>
          </div>
        </div>
        <div className="flex justify-center space-x-4 mt-6">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors ${t.button}`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
