Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels) Summary: [Constants](https://developer.android.com/reference/android/view/RoundedCorner#constants) \| [Inherited Constants](https://developer.android.com/reference/android/view/RoundedCorner#inhconstants) \| [Fields](https://developer.android.com/reference/android/view/RoundedCorner#lfields) \| [Ctors](https://developer.android.com/reference/android/view/RoundedCorner#pubctors) \| [Methods](https://developer.android.com/reference/android/view/RoundedCorner#pubmethods) \| [Inherited Methods](https://developer.android.com/reference/android/view/RoundedCorner#inhmethods)

# RoundedCorner

*** ** * ** ***

[Kotlin](https://developer.android.com/reference/kotlin/android/view/RoundedCorner "View this page in Kotlin") \|Java


`
public

final

class
RoundedCorner
`


`

extends https://developer.android.com/reference/java/lang/Object


`

`


implements

https://developer.android.com/reference/android/os/Parcelable


`

|---|---|
| [java.lang.Object](https://developer.android.com/reference/java/lang/Object) ||
| ↳ | android.view.RoundedCorner |

<br />

*** ** * ** ***

Represents a rounded corner of the display.


![A figure to describe what the rounded corner radius and the center point are.](https://developer.android.com/static/reference/android/images/rounded_corner/rounded-corner-info.png)

Note: The rounded corner formed by the radius and the center is an approximation.

`https://developer.android.com/reference/android/view/RoundedCorner` is immutable.

<br />

## Summary

| ### Constants ||
|---|---|
| `int` | `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_LEFT` The rounded corner is at the bottom-left of the screen. |
| `int` | `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_RIGHT` The rounded corner is at the bottom-right of the screen. |
| `int` | `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_LEFT` The rounded corner is at the top-left of the screen. |
| `int` | `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_RIGHT` The rounded corner is at the top-right of the screen. |

| ### Inherited constants |
|---|
| From interface `https://developer.android.com/reference/android/os/Parcelable` |---|---| | `int` | `https://developer.android.com/reference/android/os/Parcelable#CONTENTS_FILE_DESCRIPTOR` Descriptor bit used with `https://developer.android.com/reference/android/os/Parcelable#describeContents()`: indicates that the Parcelable object's flattened representation includes a file descriptor. | | `int` | `https://developer.android.com/reference/android/os/Parcelable#PARCELABLE_WRITE_RETURN_VALUE` Flag for use with `https://developer.android.com/reference/android/os/Parcelable#writeToParcel(android.os.Parcel,%20int)`: the object being written is a return value, that is the result of a function such as "`Parcelable someFunction()`", "`void someFunction(out Parcelable)`", or "`void someFunction(inout Parcelable)`". | |

| ### Fields ||
|---|---|
| ` public static final https://developer.android.com/reference/android/os/Parcelable.Creator<https://developer.android.com/reference/android/view/RoundedCorner>` | `https://developer.android.com/reference/android/view/RoundedCorner#CREATOR` |

| ### Public constructors ||
|---|---|
| ` https://developer.android.com/reference/android/view/RoundedCorner#RoundedCorner(int,%20int,%20int,%20int)(int position, int radius, int centerX, int centerY) ` Creates a `https://developer.android.com/reference/android/view/RoundedCorner`. |

| ### Public methods ||
|---|---|
| ` int` | ` https://developer.android.com/reference/android/view/RoundedCorner#describeContents()() ` Describe the kinds of special objects contained in this Parcelable instance's marshaled representation. |
| ` boolean` | ` https://developer.android.com/reference/android/view/RoundedCorner#equals(java.lang.Object)(https://developer.android.com/reference/java/lang/Object o) ` Indicates whether some other object is "equal to" this one. |
| ` https://developer.android.com/reference/android/graphics/Point` | ` https://developer.android.com/reference/android/view/RoundedCorner#getCenter()() ` Returns the circle center of a quarter circle approximation of this `https://developer.android.com/reference/android/view/RoundedCorner`. |
| ` int` | ` https://developer.android.com/reference/android/view/RoundedCorner#getPosition()() ` Get the position of this `https://developer.android.com/reference/android/view/RoundedCorner`. |
| ` int` | ` https://developer.android.com/reference/android/view/RoundedCorner#getRadius()() ` Returns the radius of a quarter circle approximation of this `https://developer.android.com/reference/android/view/RoundedCorner`. |
| ` int` | ` https://developer.android.com/reference/android/view/RoundedCorner#hashCode()() ` Returns a hash code value for the object. |
| ` https://developer.android.com/reference/java/lang/String` | ` https://developer.android.com/reference/android/view/RoundedCorner#toString()() ` Returns a string representation of the object. |
| ` void` | ` https://developer.android.com/reference/android/view/RoundedCorner#writeToParcel(android.os.Parcel,%20int)(https://developer.android.com/reference/android/os/Parcel out, int flags) ` Flatten this object in to a Parcel. |

| ### Inherited methods |
|---|---|
| From class ` https://developer.android.com/reference/java/lang/Object ` |---|---| | ` https://developer.android.com/reference/java/lang/Object` | ` https://developer.android.com/reference/java/lang/Object#clone()() ` Creates and returns a copy of this object. | | ` boolean` | ` https://developer.android.com/reference/java/lang/Object#equals(java.lang.Object)(https://developer.android.com/reference/java/lang/Object obj) ` Indicates whether some other object is "equal to" this one. | | ` void` | ` https://developer.android.com/reference/java/lang/Object#finalize()() ` Called by the garbage collector on an object when garbage collection determines that there are no more references to the object. | | ` final https://developer.android.com/reference/java/lang/Class<?>` | ` https://developer.android.com/reference/java/lang/Object#getClass()() ` Returns the runtime class of this `Object`. | | ` int` | ` https://developer.android.com/reference/java/lang/Object#hashCode()() ` Returns a hash code value for the object. | | ` final void` | ` https://developer.android.com/reference/java/lang/Object#notify()() ` Wakes up a single thread that is waiting on this object's monitor. | | ` final void` | ` https://developer.android.com/reference/java/lang/Object#notifyAll()() ` Wakes up all threads that are waiting on this object's monitor. | | ` https://developer.android.com/reference/java/lang/String` | ` https://developer.android.com/reference/java/lang/Object#toString()() ` Returns a string representation of the object. | | ` final void` | ` https://developer.android.com/reference/java/lang/Object#wait(long,%20int)(long timeoutMillis, int nanos) ` Causes the current thread to wait until it is awakened, typically by being *notified* or *interrupted*, or until a certain amount of real time has elapsed. | | ` final void` | ` https://developer.android.com/reference/java/lang/Object#wait(long)(long timeoutMillis) ` Causes the current thread to wait until it is awakened, typically by being *notified* or *interrupted*, or until a certain amount of real time has elapsed. | | ` final void` | ` https://developer.android.com/reference/java/lang/Object#wait()() ` Causes the current thread to wait until it is awakened, typically by being *notified* or *interrupted*. | ||
| From interface ` https://developer.android.com/reference/android/os/Parcelable ` |---|---| | ` abstract int` | ` https://developer.android.com/reference/android/os/Parcelable#describeContents()() ` Describe the kinds of special objects contained in this Parcelable instance's marshaled representation. | | ` abstract void` | ` https://developer.android.com/reference/android/os/Parcelable#writeToParcel(android.os.Parcel,%20int)(https://developer.android.com/reference/android/os/Parcel dest, int flags) ` Flatten this object in to a Parcel. | ||

## Constants

### POSITION_BOTTOM_LEFT

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public static final int POSITION_BOTTOM_LEFT
```

The rounded corner is at the bottom-left of the screen.

<br />

Constant Value:

3
(0x00000003)


### POSITION_BOTTOM_RIGHT

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public static final int POSITION_BOTTOM_RIGHT
```

The rounded corner is at the bottom-right of the screen.

<br />

Constant Value:

2
(0x00000002)


### POSITION_TOP_LEFT

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public static final int POSITION_TOP_LEFT
```

The rounded corner is at the top-left of the screen.

<br />

Constant Value:

0
(0x00000000)


### POSITION_TOP_RIGHT

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public static final int POSITION_TOP_RIGHT
```

The rounded corner is at the top-right of the screen.

<br />

Constant Value:

1
(0x00000001)


## Fields

### CREATOR

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public static final Creator<RoundedCorner> CREATOR
```

<br />

<br />

## Public constructors

### RoundedCorner

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public RoundedCorner (int position, 
                int radius, 
                int centerX, 
                int centerY)
```

Creates a `https://developer.android.com/reference/android/view/RoundedCorner`.

Note that this is only useful for tests. For production code, developers should always
use a `https://developer.android.com/reference/android/view/RoundedCorner` obtained from the system via
`https://developer.android.com/reference/android/view/WindowInsets#getRoundedCorner(int)` or `https://developer.android.com/reference/android/view/Display#getRoundedCorner(int)`.

<br />

<br />

| Parameters ||
|---|---|
| `position` | `int`: the position of the rounded corner. Value is one of the following: - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_LEFT` - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_RIGHT` - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_RIGHT` - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_LEFT` |
| `radius` | `int`: the radius of the rounded corner. <br /> |
| `centerX` | `int`: the x of center point of the rounded corner. <br /> |
| `centerY` | `int`: the y of center point of the rounded corner. <br /> |

## Public methods

### describeContents

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public int describeContents ()
```

Describe the kinds of special objects contained in this Parcelable
instance's marshaled representation. For example, if the object will
include a file descriptor in the output of `https://developer.android.com/reference/android/os/Parcelable#writeToParcel(android.os.Parcel,%20int)`,
the return value of this method must include the
`https://developer.android.com/reference/android/os/Parcelable#CONTENTS_FILE_DESCRIPTOR` bit.

<br />

| Returns ||
|---|---|
| `int` | a bitmask indicating the set of special object types marshaled by this Parcelable object instance. Value is either `0` or - `https://developer.android.com/reference/android/os/Parcelable#CONTENTS_FILE_DESCRIPTOR` |

### equals

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public boolean equals (Object o)
```

Indicates whether some other object is "equal to" this one.


The `equals` method implements an equivalence relation
on non-null object references:

- It is *reflexive*: for any non-null reference value `x`, `x.equals(x)` should return `true`.
- It is *symmetric*: for any non-null reference values `x` and `y`, `x.equals(y)` should return `true` if and only if `y.equals(x)` returns `true`.
- It is *transitive*: for any non-null reference values `x`, `y`, and `z`, if `x.equals(y)` returns `true` and `y.equals(z)` returns `true`, then `x.equals(z)` should return `true`.
- It is *consistent*: for any non-null reference values `x` and `y`, multiple invocations of `x.equals(y)` consistently return `true` or consistently return `false`, provided no information used in `equals` comparisons on the objects is modified.
- For any non-null reference value `x`, `x.equals(null)` should return `false`.


An equivalence relation partitions the elements it operates on
into *equivalence classes*; all the members of an
equivalence class are equal to each other. Members of an
equivalence class are substitutable for each other, at least
for some purposes.

<br />

| Parameters ||
|---|---|
| `o` | `Object`: the reference object with which to compare. <br /> |

| Returns ||
|---|---|
| `boolean` | `true` if this object is the same as the obj argument; `false` otherwise. <br /> |

### getCenter

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public Point getCenter ()
```

Returns the circle center of a quarter circle approximation of this `https://developer.android.com/reference/android/view/RoundedCorner`.

<br />

| Returns ||
|---|---|
| `https://developer.android.com/reference/android/graphics/Point` | the center point of this `https://developer.android.com/reference/android/view/RoundedCorner` in the application's coordinate. This value cannot be `null`. <br /> |

### getPosition

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public int getPosition ()
```

Get the position of this `https://developer.android.com/reference/android/view/RoundedCorner`.

<br />

| Returns ||
|---|---|
| `int` | Value is one of the following: - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_LEFT` - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_RIGHT` - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_RIGHT` - `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_LEFT` |

**See also:**

- `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_LEFT`
- `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_TOP_RIGHT`
- `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_RIGHT`
- `https://developer.android.com/reference/android/view/RoundedCorner#POSITION_BOTTOM_LEFT`

### getRadius

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public int getRadius ()
```

Returns the radius of a quarter circle approximation of this `https://developer.android.com/reference/android/view/RoundedCorner`.

<br />

| Returns ||
|---|---|
| `int` | the rounded corner radius of this `https://developer.android.com/reference/android/view/RoundedCorner`. Returns 0 if there is no rounded corner. <br /> |

### hashCode

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public int hashCode ()
```

Returns a hash code value for the object. This method is
supported for the benefit of hash tables such as those provided by
`https://developer.android.com/reference/java/util/HashMap`.


The general contract of `hashCode` is:

- Whenever it is invoked on the same object more than once during an execution of a Java application, the `hashCode` method must consistently return the same integer, provided no information used in `equals` comparisons on the object is modified. This integer need not remain consistent from one execution of an application to another execution of the same application.
- If two objects are equal according to the `https://developer.android.com/reference/java/lang/Object#equals(java.lang.Object)` method, then calling the `hashCode` method on each of the two objects must produce the same integer result.
- It is *not* required that if two objects are unequal according to the `https://developer.android.com/reference/java/lang/Object#equals(java.lang.Object)` method, then calling the `hashCode` method on each of the two objects must produce distinct integer results. However, the programmer should be aware that producing distinct integer results for unequal objects may improve the performance of hash tables.

<br />

<br />

| Returns ||
|---|---|
| `int` | a hash code value for this object. <br /> |

### toString

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public String toString ()
```

Returns a string representation of the object.

<br />

| Returns ||
|---|---|
| `https://developer.android.com/reference/java/lang/String` | a string representation of the object. <br /> |

### writeToParcel

Added in [API level 31](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

```
public void writeToParcel (Parcel out, 
                int flags)
```

Flatten this object in to a Parcel.

<br />

| Parameters ||
|---|---|
| `out` | `Parcel`: This value cannot be `null`. <br /> |
| `flags` | `int`: Additional flags about how the object should be written. May be 0 or `https://developer.android.com/reference/android/os/Parcelable#PARCELABLE_WRITE_RETURN_VALUE`. Value is either `0` or a combination of the following: - `https://developer.android.com/reference/android/os/Parcelable#PARCELABLE_WRITE_RETURN_VALUE` |