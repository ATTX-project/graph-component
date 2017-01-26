import os
import json
import jsonschema
import falcon


def validate(schema):
    """Validate Schema."""
    def decorator(func):
        def wrapper(self, req, resp, *args, **kwargs):
            try:
                raw_json = req.stream.read()
                obj = json.loads(raw_json.decode('utf-8'))
            except Exception:
                raise falcon.HTTPBadRequest(
                    'Invalid data',
                    'Could not properly parse the provided data as JSON'
                )

            try:
                jsonschema.validate(obj, schema)
            except jsonschema.ValidationError as e:
                raise falcon.HTTPBadRequest(
                    'Failed data validation',
                    e.message
                )

            return func(self, req, resp, *args, parsed=obj, **kwargs)
        return wrapper
    return decorator


def load_schema(name):
    """Load schema for validation of the response."""
    module_path = os.path.dirname(__file__)
    path = os.path.join(module_path, '{}.json'.format(name))

    with open(os.path.abspath(path), 'r') as fp:
        data = fp.read()

    return json.loads(data)
