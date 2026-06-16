let dbInstance = null
const DB_NAME = 'solar_ops_inspection.db'
const DB_PATH = '_doc/solar_ops_inspection.db'

const TABLES = [
  'inspection_task',
  'inspection_task_item',
  'inspection_result',
  'inspection_result_item',
  'inspection_photo',
  'inspection_audio',
  'inspection_report'
]

export function openDB() {
  return new Promise((resolve, reject) => {
    if (dbInstance) {
      resolve(dbInstance)
      return
    }
    
    // #ifdef APP-PLUS
    plus.sqlite.openDatabase({
      name: DB_NAME,
      path: DB_PATH,
      success: () => {
        dbInstance = { name: DB_NAME, path: DB_PATH }
        initTables().then(() => {
          resolve(dbInstance)
        }).catch(reject)
      },
      fail: (err) => {
        console.error('打开数据库失败', err)
        reject(err)
      }
    })
    // #endif
    
    // #ifndef APP-PLUS
    dbInstance = { name: DB_NAME, isMock: true }
    initMockDB()
    resolve(dbInstance)
    // #endif
  }
}

function initMockDB() {
  if (!uni.$inspectionMockDB) {
    uni.$inspectionMockDB = {}
    TABLES.forEach(table => {
      uni.$inspectionMockDB[table] = []
    })
    uni.$inspectionMockDB.autoIncrement = {}
    TABLES.forEach(table => {
      uni.$inspectionMockDB.autoIncrement[table] = 0
    })
  }
}

function initTables() {
  return new Promise((resolve, reject) => {
    const sqlList = [
      `CREATE TABLE IF NOT EXISTS inspection_task (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        task_no TEXT,
        task_name TEXT,
        station_id INTEGER,
        station_name TEXT,
        task_type INTEGER,
        priority INTEGER,
        plan_start_time TEXT,
        plan_end_time TEXT,
        actual_start_time TEXT,
        actual_end_time TEXT,
        status INTEGER,
        assignee_id INTEGER,
        assignee_name TEXT,
        description TEXT,
        remark TEXT,
        is_deleted INTEGER DEFAULT 0,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP,
        sync_status INTEGER DEFAULT 1
      )`,
      `CREATE TABLE IF NOT EXISTS inspection_task_item (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        task_item_id INTEGER,
        task_id INTEGER,
        item_id INTEGER,
        item_code TEXT,
        item_name TEXT,
        item_type INTEGER,
        asset_id INTEGER,
        asset_name TEXT,
        asset_code TEXT,
        standard_value TEXT,
        min_value REAL,
        max_value REAL,
        unit TEXT,
        is_required INTEGER,
        sort_order INTEGER,
        description TEXT,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP
      )`,
      `CREATE TABLE IF NOT EXISTS inspection_result (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        result_no TEXT,
        task_id INTEGER,
        task_no TEXT,
        station_id INTEGER,
        station_name TEXT,
        inspector_id INTEGER,
        inspector_name TEXT,
        start_time TEXT,
        end_time TEXT,
        total_items INTEGER,
        normal_items INTEGER,
        abnormal_items INTEGER,
        result_status INTEGER,
        overall_remark TEXT,
        longitude REAL,
        latitude REAL,
        is_offline INTEGER DEFAULT 1,
        upload_time TEXT,
        sync_status INTEGER DEFAULT 0,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP
      )`,
      `CREATE TABLE IF NOT EXISTS inspection_result_item (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        result_id INTEGER,
        task_item_id INTEGER,
        item_id INTEGER,
        item_name TEXT,
        item_type INTEGER,
        asset_id INTEGER,
        asset_name TEXT,
        asset_code TEXT,
        check_value TEXT,
        standard_value TEXT,
        is_normal INTEGER,
        abnormal_desc TEXT,
        remark TEXT,
        check_time TEXT,
        longitude REAL,
        latitude REAL,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP
      )`,
      `CREATE TABLE IF NOT EXISTS inspection_photo (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        photo_no TEXT,
        result_id INTEGER,
        result_item_id INTEGER,
        task_id INTEGER,
        asset_id INTEGER,
        photo_type INTEGER,
        photo_url TEXT,
        thumbnail_url TEXT,
        file_size INTEGER,
        watermark_time TEXT,
        longitude REAL,
        latitude REAL,
        has_watermark INTEGER,
        remark TEXT,
        is_offline INTEGER DEFAULT 1,
        sync_status INTEGER DEFAULT 0,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP
      )`,
      `CREATE TABLE IF NOT EXISTS inspection_audio (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        audio_no TEXT,
        result_id INTEGER,
        result_item_id INTEGER,
        task_id INTEGER,
        asset_id INTEGER,
        audio_url TEXT,
        file_size INTEGER,
        duration INTEGER,
        record_time TEXT,
        longitude REAL,
        latitude REAL,
        remark TEXT,
        is_offline INTEGER DEFAULT 1,
        sync_status INTEGER DEFAULT 0,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP
      )`,
      `CREATE TABLE IF NOT EXISTS inspection_report (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        report_no TEXT,
        report_title TEXT,
        report_type INTEGER,
        task_id INTEGER,
        result_id INTEGER,
        station_id INTEGER,
        station_name TEXT,
        total_score REAL,
        health_level INTEGER,
        total_items INTEGER,
        pass_rate REAL,
        abnormal_count INTEGER,
        problem_summary TEXT,
        suggestions TEXT,
        report_content TEXT,
        generated_time TEXT,
        generator_id INTEGER,
        generator_name TEXT,
        is_offline INTEGER DEFAULT 0,
        sync_status INTEGER DEFAULT 0,
        create_time TEXT DEFAULT CURRENT_TIMESTAMP,
        update_time TEXT DEFAULT CURRENT_TIMESTAMP
      )`
    ]
    
    executeSqlBatch(sqlList).then(resolve).catch(reject)
  })
}

export function executeSql(sql, args = []) {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    openDB().then(() => {
      plus.sqlite.executeSql({
        name: DB_NAME,
        sql: sql,
        args: args,
        success: (res) => {
          resolve(res)
        },
        fail: (err) => {
          console.error('SQL执行失败', sql, err)
          reject(err)
        }
      })
    }).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    setTimeout(() => {
      resolve({ affectedRows: 0 })
    }, 10)
    // #endif
  })
}

export function selectSql(sql, args = []) {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    openDB().then(() => {
      plus.sqlite.selectSql({
        name: DB_NAME,
        sql: sql,
        args: args,
        success: (res) => {
          resolve(res || [])
        },
        fail: (err) => {
          console.error('SQL查询失败', sql, err)
          reject(err)
        }
      })
    }).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    setTimeout(() => {
      resolve([])
    }, 10)
    // #endif
  })
}

export function executeSqlBatch(sqlList) {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    openDB().then(() => {
      let completed = 0
      let hasError = false
      
      sqlList.forEach((sql) => {
        plus.sqlite.executeSql({
          name: DB_NAME,
          sql: sql,
          success: () => {
            completed++
            if (completed === sqlList.length && !hasError) {
              resolve()
            }
          },
          fail: (err) => {
            hasError = true
            console.error('批量SQL执行失败', sql, err)
            reject(err)
          }
        })
      })
    }).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    setTimeout(resolve, 10)
    // #endif
  })
}

export function insert(tableName, data) {
  return new Promise((resolve, reject) => {
    const keys = Object.keys(data)
    const values = Object.values(data)
    const placeholders = keys.map(() => '?').join(', ')
    const columns = keys.join(', ')
    
    const sql = `INSERT INTO ${tableName} (${columns}) VALUES (${placeholders})`
    
    // #ifdef APP-PLUS
    executeSql(sql, values).then(() => {
      plus.sqlite.selectSql({
        name: DB_NAME,
        sql: 'SELECT last_insert_rowid() as id',
        success: (result) => {
          const newId = result && result.length > 0 ? result[0].id : 0
          resolve(newId)
        },
        fail: (err) => {
          console.error('获取插入ID失败', err)
          resolve(0)
        }
      })
    }).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    const mockDB = uni.$inspectionMockDB
    if (!mockDB[tableName]) {
      mockDB[tableName] = []
      mockDB.autoIncrement[tableName] = 0
    }
    const id = ++mockDB.autoIncrement[tableName]
    const now = new Date().toISOString()
    const record = { ...data, id, create_time: now, update_time: now }
    mockDB[tableName].push(record)
    setTimeout(() => resolve(id), 10)
    // #endif
  })
}

export function update(tableName, data, where, whereArgs = []) {
  return new Promise((resolve, reject) => {
    const sets = Object.keys(data).map(key => `${key} = ?`).join(', ')
    const values = Object.values(data)
    const allArgs = [...values, ...whereArgs]
    
    const sql = `UPDATE ${tableName} SET ${sets} WHERE ${where}`
    
    // #ifdef APP-PLUS
    executeSql(sql, allArgs).then(resolve).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    resolve({ affectedRows: 0 })
    // #endif
  })
}

export function query(tableName, where = '', whereArgs = [], orderBy = '', limit = '') {
  return new Promise((resolve, reject) => {
    let sql = `SELECT * FROM ${tableName}`
    if (where) {
      sql += ` WHERE ${where}`
    }
    if (orderBy) {
      sql += ` ORDER BY ${orderBy}`
    }
    if (limit) {
      sql += ` LIMIT ${limit}`
    }
    
    // #ifdef APP-PLUS
    selectSql(sql, whereArgs).then(resolve).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    const mockDB = uni.$inspectionMockDB
    const table = mockDB[tableName] || []
    setTimeout(() => resolve([...table]), 10)
    // #endif
  })
}

export function queryOne(tableName, where = '', whereArgs = []) {
  return query(tableName, where, whereArgs, '', '1').then(results => {
    return results && results.length > 0 ? results[0] : null
  })
}

export function remove(tableName, where, whereArgs = []) {
  return new Promise((resolve, reject) => {
    const sql = `DELETE FROM ${tableName} WHERE ${where}`
    // #ifdef APP-PLUS
    executeSql(sql, whereArgs).then(resolve).catch(reject)
    // #endif
    
    // #ifndef APP-PLUS
    resolve()
    // #endif
  })
}

export function beginTransaction() {
  return executeSql('BEGIN TRANSACTION')
}

export function commitTransaction() {
  return executeSql('COMMIT')
}

export function rollbackTransaction() {
  return executeSql('ROLLBACK')
}

export function closeDB() {
  return new Promise((resolve) => {
    if (!dbInstance) {
      resolve()
      return
    }
    // #ifdef APP-PLUS
    plus.sqlite.closeDatabase({
      name: DB_NAME,
      success: () => {
        dbInstance = null
        resolve()
      },
      fail: () => {
        dbInstance = null
        resolve()
      }
    })
    // #endif
    // #ifndef APP-PLUS
    dbInstance = null
    resolve()
    // #endif
  })
}

export default {
  openDB,
  executeSql,
  selectSql,
  executeSqlBatch,
  insert,
  update,
  query,
  queryOne,
  remove,
  beginTransaction,
  commitTransaction,
  rollbackTransaction,
  closeDB
}
