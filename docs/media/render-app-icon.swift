// Renders iosApp's AppIcon-1024.png.
//
//     swift docs/media/render-app-icon.swift \
//       iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png
//
// Kept as source rather than only as a PNG, the same way notion-flow.png keeps
// notion-flow.html: the icon is two paths and a palette, and a binary nobody
// can regenerate is the thing that drifts from the palette it came from.
//
// The geometry is ic_duskread_mark_ink.xml's, and so is the palette — the
// launcher icon and splash are Ink's on every platform, always, whatever
// scheme the app itself is showing.

import AppKit
import CoreGraphics
import Foundation

// Ink's palette, the same values ic_duskread_mark_ink.xml uses. Built in the
// context's own sRGB space rather than with CGColor(red:green:blue:alpha:),
// which lands in generic RGB and converts on the way in — #161616 came out
// as #1D1D1D that way, which is not a colour anything in this app uses.
let space = CGColorSpace(name: CGColorSpace.sRGB)!
func srgb(_ hex: UInt32) -> CGColor {
    CGColor(colorSpace: space, components: [
        CGFloat((hex >> 16) & 0xFF) / 255,
        CGFloat((hex >> 8) & 0xFF) / 255,
        CGFloat(hex & 0xFF) / 255,
        1,
    ])!
}
let ground = srgb(0x161616)
let ink = srgb(0xDCDCDC)

let side = 1024
let viewport: CGFloat = 108

guard let context = CGContext(
    data: nil, width: side, height: side, bitsPerComponent: 8, bytesPerRow: 0,
    space: space, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
) else { exit(1) }

context.setFillColor(ground)
context.fill(CGRect(x: 0, y: 0, width: side, height: side))

// SVG space: y down, 108 units square. The Android vector also scales the
// mark to 0.85 about its centre; that is its adaptive-icon safe zone, which a
// full-bleed iOS icon does not have — so the mark stays at full size here, the
// same as the icon this replaces.
context.translateBy(x: 0, y: CGFloat(side))
context.scaleBy(x: 1, y: -1)
context.scaleBy(x: CGFloat(side) / viewport, y: CGFloat(side) / viewport)

// The bar: M40,38 a14,14 0 0 1 28,0 v36 a14,14 0 0 1 -28,0 z — semicircular
// caps of r14 on a 28-wide body, which is a capsule from y24 to y88.
let bar = CGPath(
    roundedRect: CGRect(x: 40, y: 24, width: 28, height: 64),
    cornerWidth: 14, cornerHeight: 14, transform: nil
)
context.setFillColor(ink)
context.addPath(bar)
context.fillPath()

// The bite: M44,32 a16,16 0 0 1 32,0 a16,16 0 0 1 -32,0 z — a circle at
// (60,32) r16, painted in the ground so the mark reads as one shape.
context.setFillColor(ground)
context.addEllipse(in: CGRect(x: 44, y: 16, width: 32, height: 32))
context.fillPath()

guard let image = context.makeImage() else { exit(1) }
let rep = NSBitmapImageRep(cgImage: image)
guard let data = rep.representation(using: .png, properties: [:]) else { exit(1) }
try data.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
