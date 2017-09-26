package metadoc

import org.scalatest.FunSuite
import monaco.Range

class NavigationTest extends FunSuite {
  test("Navigation.parseState") {
    val state = Navigation.currentState(None, "/path")
    assert(state.isDefined)
    assert(state.get.path == "/path")
    assert(state.get.selection.isEmpty)

    val stateWithSelection = Navigation.parseState("/path2#L11")
    assert(stateWithSelection.isDefined)
    assert(stateWithSelection.get.path == "/path2")
    assert(stateWithSelection.get.selection == Some(Navigation.Selection(11, 1, 12, 1)))

    assert(Navigation.parseState("").isEmpty)
    assert(Navigation.parseState("#/path2#L11").isEmpty)
  }

  test("Navigation.curentState") {
    val stateFromHash = Navigation.currentState(None, "/path")
    assert(stateFromHash.isDefined)
    assert(stateFromHash.get.path == "/path")
    assert(stateFromHash.get.selection.isEmpty)

    val stateFromHashWithSelection = Navigation.currentState(None, "/path2#L11")
    assert(stateFromHashWithSelection.isDefined)
    assert(stateFromHashWithSelection.get.path == "/path2")
    assert(stateFromHashWithSelection.get.selection == Some(Navigation.Selection(11, 1, 12, 1)))

    val stateFromHistory = Navigation.currentState(stateFromHash, "/other-path")
    assert(stateFromHistory.isDefined)
    assert(stateFromHistory.get.path == "/path")
    assert(stateFromHistory.get.selection.isEmpty)
  }

  test("Navigation.Selection.toString") {
    assert(Navigation.Selection(11, 1, 12, 1).toString == "L11")
    assert(Navigation.Selection(11, 1, 12, 4).toString == "L11-L12C4")
    assert(Navigation.Selection(11, 1, 12, 4).toString == "L11-L12C4")
    assert(Navigation.Selection(11, 2, 12, 4).toString == "L11C2-L12C4")
    assert(Navigation.Selection(11, 2, 12, 1).toString == "L11C2-L12")
  }

  test("Navigation.parseSelection") {
    val str = "L10C4-L14C20"
    val Some(parsed) = Navigation.parseSelection(str)

    assert(parsed == Navigation.Selection(10, 4, 14, 20))
    assert(parsed.toString == str)

    assert(Navigation.parseSelection("L10-C1") == None)
  }

  test("Navigation.parseSelection normalization") {
    val selection = Navigation.Selection(1, 1, 2, 1)
    assert(Navigation.parseSelection("L1") == Some(selection))
    assert(Navigation.parseSelection("L1-L2") == Some(selection))
    assert(Navigation.parseSelection("L1C1-L2") == Some(selection))
    assert(Navigation.parseSelection("L1-L2C1") == Some(selection))
    assert(Navigation.parseSelection("L1C1-L2C1") == Some(selection))
  }

  test("Navigation.parseSelection roundtrip") {
    val samples =
      """L1
        |L1-L3
        |L1C2-L2
        |L1-L1C2
        |L110-L112C25
        |
        |L10-L2
        |"""

    samples.stripMargin.split('\n').filter(_.nonEmpty).foreach { selection =>
      assert(
        Navigation.parseSelection(selection).map(_.toString) == Some(selection)
      )
    }
  }
}
