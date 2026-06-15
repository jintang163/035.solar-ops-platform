const TOKEN_KEY = 'solar_ops_token'
const USER_KEY = 'solar_ops_user'
const WORKSPACE_KEY = 'solar_ops_workspace'
const CURRENT_STATION_KEY = 'solar_ops_current_station'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser() {
  const userStr = localStorage.getItem(USER_KEY)
  return userStr ? JSON.parse(userStr) : null
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUser() {
  localStorage.removeItem(USER_KEY)
}

export function getWorkspace() {
  const workspaceStr = localStorage.getItem(WORKSPACE_KEY)
  return workspaceStr ? JSON.parse(workspaceStr) : null
}

export function setWorkspace(workspace) {
  localStorage.setItem(WORKSPACE_KEY, JSON.stringify(workspace))
}

export function removeWorkspace() {
  localStorage.removeItem(WORKSPACE_KEY)
}

export function getCurrentStationId() {
  const stationId = localStorage.getItem(CURRENT_STATION_KEY)
  return stationId ? (stationId === 'null' || stationId === 'undefined' ? null : stationId) : null
}

export function setCurrentStationId(stationId) {
  localStorage.setItem(CURRENT_STATION_KEY, stationId == null ? 'null' : String(stationId))
}

export function removeCurrentStationId() {
  localStorage.removeItem(CURRENT_STATION_KEY)
}

export function isSuperAdmin() {
  const user = getUser()
  return user?.isAdmin === 1
}

export function clearAuth() {
  removeToken()
  removeUser()
  removeWorkspace()
  removeCurrentStationId()
}
