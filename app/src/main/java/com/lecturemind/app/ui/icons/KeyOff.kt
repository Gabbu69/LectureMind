/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lecturemind.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.KeyOff: ImageVector
    get() {
        if (_keyOff != null) {
            return _keyOff!!
        }
        _keyOff = materialIcon(name = "Outlined.KeyOff") {
            materialPath {
                moveTo(10.7f, 13.53f)
                lineToRelative(-1.71f, -1.71f)
                curveTo(9.0f, 11.88f, 9.0f, 11.94f, 9.0f, 12.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                curveToRelative(0.06f, 0.0f, 0.12f, 0.0f, 0.18f, 0.01f)
                lineTo(5.47f, 8.3f)
                curveTo(4.02f, 8.9f, 3.0f, 10.33f, 3.0f, 12.0f)
                curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
                curveTo(8.67f, 16.0f, 10.1f, 14.98f, 10.7f, 13.53f)
                close()
                moveTo(12.19f, 15.02f)
                curveTo(11.15f, 16.8f, 9.21f, 18.0f, 7.0f, 18.0f)
                curveToRelative(-3.31f, 0.0f, -6.0f, -2.69f, -6.0f, -6.0f)
                curveToRelative(0.0f, -2.21f, 1.2f, -4.15f, 2.98f, -5.19f)
                lineTo(1.39f, 4.22f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(18.38f, 18.38f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(12.19f, 15.02f)
                close()
                moveTo(16.26f, 13.43f)
                lineToRelative(1.24f, -0.93f)
                lineToRelative(1.81f, 1.36f)
                lineTo(21.17f, 12.0f)
                lineToRelative(-1.0f, -1.0f)
                lineToRelative(-6.34f, 0.0f)
                lineToRelative(-2.0f, -2.0f)
                lineTo(21.0f, 9.0f)
                lineToRelative(0.0f, 0.0f)
                lineToRelative(3.0f, 3.0f)
                lineToRelative(-4.5f, 4.5f)
                lineToRelative(-0.69f, -0.51f)
                lineTo(16.26f, 13.43f)
                close()
            }
        }
        return _keyOff!!
    }

private var _keyOff: ImageVector? = null
