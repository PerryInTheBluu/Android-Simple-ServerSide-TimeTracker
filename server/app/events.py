"""Real-time WebSocket event broadcaster for Web clients, dashboards, and Home Assistant."""
import asyncio
import logging
from typing import Any, Dict, List, Optional, Set
from fastapi import WebSocket

logger = logging.getLogger(__name__)


class ConnectionManager:
    """Manages active WebSocket connections grouped by user_id."""

    def __init__(self):
        self.active_connections: Dict[str, Set[WebSocket]] = {}
        self.lock = asyncio.Lock()

    async def connect(self, user_id: str, websocket: WebSocket):
        await websocket.accept()
        async with self.lock:
            if user_id not in self.active_connections:
                self.active_connections[user_id] = set()
            self.active_connections[user_id].add(websocket)
        logger.info("WebSocket connected for user %s (total: %d)", user_id, len(self.active_connections[user_id]))

    async def disconnect(self, user_id: str, websocket: WebSocket):
        async with self.lock:
            if user_id in self.active_connections:
                self.active_connections[user_id].discard(websocket)
                if not self.active_connections[user_id]:
                    del self.active_connections[user_id]
        logger.info("WebSocket disconnected for user %s", user_id)

    async def broadcast_to_user(self, user_id: str, message: Dict[str, Any]):
        targets: List[WebSocket] = []
        async with self.lock:
            if user_id in self.active_connections:
                targets = list(self.active_connections[user_id])
        if not targets:
            return

        dead_sockets: List[WebSocket] = []
        for connection in targets:
            try:
                await connection.send_json(message)
            except Exception as exc:
                logger.debug("Error sending WebSocket message: %s", exc)
                dead_sockets.append(connection)

        if dead_sockets:
            async with self.lock:
                if user_id in self.active_connections:
                    for dead in dead_sockets:
                        self.active_connections[user_id].discard(dead)
                    if not self.active_connections[user_id]:
                        del self.active_connections[user_id]

    def broadcast_sync(self, user_id: str, message: Dict[str, Any]):
        """Safe synchronous broadcast trigger from standard sync request handlers."""
        try:
            loop = asyncio.get_running_loop()
            if loop.is_running():
                asyncio.create_task(self.broadcast_to_user(user_id, message))
        except RuntimeError:
            pass


manager = ConnectionManager()
