from pkgutil import extend_path

# To handle the situation when 'datalore' package is shared my modules in different locations.
__path__ = extend_path(__path__, __name__)

from ._version import __version__
from .plot import *
from ._global_settings import DatalorePlotSettings

__all__ = plot.__all__ + ['DatalorePlotSettings']