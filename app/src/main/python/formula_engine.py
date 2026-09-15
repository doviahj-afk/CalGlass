"""
Runs user-written Python "formulas" inside the app.

Contract with the Kotlin side (see PythonBridge.kt):
    run_formula(code: str, variables_json: str) -> str (JSON)

The user writes ordinary Python. Whatever value ends up bound to a variable
named `result` after the code runs is what gets shown on the calculator
display. Anything the code prints is captured too, so users can debug their
formulas (e.g. print(x) inside a loop) and still see it.

Example formula a user might type:

    # compound interest
    result = principal * (1 + rate / n) ** (n * years)

Or something loopier:

    total = 0
    for i in range(1, n + 1):
        total += i ** 2
    result = total
"""

import io
import json
import math
import contextlib
import traceback

# Names available inside every formula without the user needing to `import` them.
_SAFE_BUILTINS = {
    "abs": abs, "round": round, "min": min, "max": max, "sum": sum,
    "pow": pow, "range": range, "len": len, "sorted": sorted,
    "int": int, "float": float, "str": str, "bool": bool, "list": list,
    "dict": dict, "tuple": tuple, "set": set, "enumerate": enumerate,
    "zip": zip, "map": map, "filter": filter, "print": print,
    "True": True, "False": False, "None": None,
}


def _build_namespace(variables):
    ns = {"__builtins__": _SAFE_BUILTINS, "math": math}
    # Expose common math functions/constants at top level too (sin, sqrt, pi ...)
    for name in dir(math):
        if not name.startswith("_"):
            ns[name] = getattr(math, name)
    ns.update(variables)
    return ns


def run_formula(code: str, variables_json: str = "{}") -> str:
    """
    Executes `code` with the given variables in scope.
    Returns a JSON string: {"ok": true, "result": ..., "stdout": "..."}
                         or {"ok": false, "error": "...", "stdout": "..."}
    """
    try:
        variables = json.loads(variables_json) if variables_json else {}
    except json.JSONDecodeError as e:
        return json.dumps({"ok": False, "error": f"Bad variables JSON: {e}", "stdout": ""})

    namespace = _build_namespace(variables)
    stdout_buffer = io.StringIO()

    try:
        with contextlib.redirect_stdout(stdout_buffer):
            exec(code, namespace)

        if "result" not in namespace:
            return json.dumps({
                "ok": False,
                "error": "Formula didn't set a variable named 'result'.",
                "stdout": stdout_buffer.getvalue(),
            })

        value = namespace["result"]

        # Keep the payload JSON-safe; fall back to str() for anything exotic.
        try:
            json.dumps(value)
            safe_value = value
        except TypeError:
            safe_value = str(value)

        return json.dumps({
            "ok": True,
            "result": safe_value,
            "stdout": stdout_buffer.getvalue(),
        })

    except Exception:
        return json.dumps({
            "ok": False,
            "error": traceback.format_exc(limit=2),
            "stdout": stdout_buffer.getvalue(),
        })
