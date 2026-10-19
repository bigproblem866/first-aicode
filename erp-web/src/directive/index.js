import hasPermi from './hasPermi'
import hasRole from './hasRole'

/**
 * 指令统一入口：app.use(directives)
 */
export default {
  install(app) {
    app.directive('hasPermi', hasPermi)
    app.directive('hasRole', hasRole)
  }
}
