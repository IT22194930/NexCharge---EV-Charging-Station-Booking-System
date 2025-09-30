import React from "react";

export default function DeleteConfirmDialog({
  visible,
  cancelDelete,
  confirmDelete,
  handleDeleteBackdropClick
}) {
  if (!visible) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={handleDeleteBackdropClick}
    >
      <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
        <div className="flex items-center justify-center w-12 h-12 mx-auto bg-red-100 rounded-full">
          <svg
            className="w-6 h-6 text-red-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.268 18.5c-.77.833.192 2.5 1.732 2.5z"
            ></path>
          </svg>
        </div>
        <div className="mt-3 text-center">
          <h3 className="text-lg font-medium text-gray-900">Delete Booking</h3>
          <div className="mt-2">
            <p className="text-sm text-gray-500">
              Are you sure you want to permanently delete this booking? This action cannot be undone.
            </p>
          </div>
        </div>
        <div className="flex justify-center space-x-4 mt-6">
          <button
            onClick={cancelDelete}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={confirmDelete}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}
