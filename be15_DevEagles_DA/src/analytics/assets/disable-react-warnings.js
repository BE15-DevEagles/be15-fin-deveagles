// Suppress specific React 18 deprecation warnings for 3rd-party libs we cannot control
(function () {
  const originalWarn = console.warn;
  console.warn = function (message, ...args) {
    if (
      typeof message === "string" &&
      message.includes("componentWillReceiveProps has been renamed")
    ) {
      return; // ignore this warning
    }
    originalWarn.call(console, message, ...args);
  };
})();
