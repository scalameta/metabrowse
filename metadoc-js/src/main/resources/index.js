/*
 * Register external libraries for use by the Scala.js bundle.
 */

// Scala tokenizer and syntax highlighting
window.ScalaLanguage = require('./scala.ts')

/*
 * Load additional resources.
 */

// Import special entry point to only include a subset of the features & languages
// as configured via the Monaco Webpack plugin.
window.monaco = require('node_modules/monaco-editor/esm/vs/editor/editor.api.js');

require('node_modules/material-components-web/dist/material-components-web.css')
require('./index.css')
