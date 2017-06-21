/*
 * Register external libraries for use by the Scala.js bundle.
 */

// Scala tokenizer and syntax highlighting
window.ScalaLanguage = require('./scala.ts');

/*
 * Load additional resources.
 */

require('./index.scss')
