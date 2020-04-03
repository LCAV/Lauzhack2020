from abc import ABC, abstractmethod
from threading import Thread


class StoppableThread(Thread, ABC):
    def __init__(self):
        Thread.__init__(self)

    @abstractmethod
    def stop(self):
        pass

    @abstractmethod
    def is_running(self):
        pass
