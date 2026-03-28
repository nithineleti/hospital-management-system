(function() {
  'use strict';

  // AI Agent Widget for Hospital Dashboard
  function createAIWidget() {
    const widget = document.createElement('div');
    widget.id = 'siri-ai-widget';
    widget.innerHTML = `
      <button id="siri-toggle" class="siri-toggle">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
        </svg>
        <span>Siri AI</span>
      </button>
      <div id="siri-frame-container" class="siri-frame-container">
        <iframe id="siri-iframe" src="chat.html"></iframe>
      </div>
      <button class="siri-close" id="siri-close">×</button>
    `;

    const style = document.createElement('style');
    style.textContent = `#siri-ai-widget {
        position: fixed;
        bottom: 20px;
        right: 20px;
        z-index: 10000;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      .siri-toggle {
        position: relative;
        background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
        color: white;
        border: none;
        border-radius: 50px;
        padding: 12px 20px;
        box-shadow: 0 8px 32px rgba(0, 212, 255, 0.3);
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        font-size: 14px;
        transition: all 0.3s ease;
        backdrop-filter: blur(10px);
      }

      .siri-toggle:hover {
        transform: translateY(-2px);
        box-shadow: 0 12px 40px rgba(0, 212, 255, 0.4);
      }

      .siri-frame-container {
        position: absolute;
        bottom: 70px;
        right: 0;
        width: 420px;
        height: 600px;
        background: rgba(15, 20, 25, 0.98);
        border-radius: 20px;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
        opacity: 0;
        transform: scale(0.8) translateY(20px);
        visibility: hidden;
        transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
        overflow: hidden;
        border: 1px solid rgba(0, 212, 255, 0.3);
      }

      .siri-frame-container.active {
        opacity: 1;
        transform: scale(1) translateY(0);
        visibility: visible;
      }

      #siri-iframe {
        width: 100%;
        height: 100%;
        border: none;
        border-radius: 20px;
        background: #f8f9fa;
      }

      .siri-close {
        position: absolute;
        top: 12px;
        right: 12px;
        background: rgba(255, 255, 255, 0.2);
        border: none;
        border-radius: 50%;
        width: 32px;
        height: 32px;
        color: white;
        font-size: 20px;
        font-weight: bold;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
        backdrop-filter: blur(10px);
        z-index: 10001;
      }

      .siri-close:hover {
        background: rgba(255, 0, 110, 0.3);
        transform: rotate(90deg);
      }

      @media (max-width: 768px) {
        .siri-frame-container {
          width: calc(100vw - 40px);
          height: calc(100vh - 120px);
          right: 20px;
          bottom: 80px;
        }
      }
    `;
    widget.appendChild(style);
    document.body.appendChild(widget);

    const toggleBtn = document.getElementById('siri-toggle');
    const frameContainer = document.getElementById('siri-frame-container');
    const iframe = document.getElementById('siri-iframe');
    const closeBtn = document.getElementById('siri-close');

    window.toggleSiriFrame = function() {
      const isActive = frameContainer.classList.contains('active');
      if (isActive) {
        frameContainer.classList.remove('active');
        toggleBtn.style.display = 'flex';
        closeBtn.style.opacity = '0';
      } else {
        frameContainer.classList.add('active');
        toggleBtn.style.display = 'none';
        iframe.src = iframe.src;  // Reload iframe
        iframe.focus();
      }
    };

    toggleBtn.addEventListener('click', window.toggleSiriFrame);
    closeBtn.addEventListener('click', window.toggleSiriFrame);

    // Close on Escape
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape' && frameContainer.classList.contains('active')) {
        window.toggleSiriFrame();
      }
    });
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createAIWidget);
  } else {
    createAIWidget();
  }
})();

