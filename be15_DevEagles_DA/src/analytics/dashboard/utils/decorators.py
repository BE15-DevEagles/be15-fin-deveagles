from functools import wraps


def ensure_results(func):
    """Decorator for Dash callbacks (instance methods).

    Ensures `self._ensure_results_loaded(store_data)` is invoked **before** the original
    callback logic. Works even when Dash passes only positional args by inferring
    `self` from the first positional argument.
    """

    @wraps(func)
    def wrapper(*args, **kwargs):
        if not args:
            return func(*args, **kwargs)

        self = args[0]

        # Dash usually passes store_data as first arg after self (positional) or kwarg
        store_data = None
        if len(args) >= 2:
            store_data = args[1]
        if "store_data" in kwargs:
            store_data = kwargs["store_data"]

        if hasattr(self, "_ensure_results_loaded"):
            self._ensure_results_loaded(store_data)

        return func(*args, **kwargs)

    return wrapper 