/*
 * Register external libraries for use by the Scala.js bundle.
 */

// Scala tokenizer and syntax highlighting
window.ScalaLanguage = require('./scala.ts')

/*
 * Load additional resources.
 */

require('node_modules/material-design-icons/iconfont/material-icons.css')
require('node_modules/material-components-web/dist/material-components-web.css')
require('./index.css')
