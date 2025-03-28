package de.moritzhank.cmftbl.smt.solver.misc

import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

/**
 * Returns the absolute path of [path] from the project dir, guaranteed without '\' or '/' at the
 * end.
 */
fun getAbsolutePathFromProjectDir(path: String) =
    Paths.get("${Paths.get("").absolutePathString()}${File.separator}$path")
        .absolutePathString()
        .dropLastWhile { it == File.separatorChar }
