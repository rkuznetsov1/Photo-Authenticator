package com.apps.elliotgrin.authenticator.ssdeep;

/*
 * Copyright (C) 2013 Marius Mailat http://fastlink2.com/contact.htm
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
 * 
 * EditDistance
 * 
 * This edit distance code is taken from trn3.6. A few minor modifications have
 * been made by Andrew Tridgell <tridge@samba.org> for use in spamsum.
 * 
 * A C to Java port of edit_dist -- returns the minimum edit distance between
 * two strings originally program by: Mark Maimone, CMU Computer Science, 13 Nov
 * 89 Last Modified: 28 Jan 90
 * 
 */

import static java.lang.Math.min;
import static java.lang.Math.max;

public class EditDistance {
	private static final int MIN_DIST = 100;

	/*
	 * Use a less-general version of the routine, one that's better for trn. All
	 * change costs are 1, and it's okay to terminate if the edit distance is
	 * known to exceed MIN_DIST
	 */

	// worry about allocating more memory only when this # of bytes is exceeded
	private static final int THRESHOLD = 4000;

	private static final int STRLENTHRESHOLD = ((int) ((THRESHOLD
			/ (Integer.SIZE / 8) - 3) / 2));

	// #define min3(x,y,z) (_mx = (x), _my = (y), _mz = (z), (_mx < _my ? (_mx <
	// _mz ? _mx : _mz) : (_mz < _my) ? _mz : _my))
	private static int min3(int x, int y, int z) {
		return min(min(x, y), z);
	}

	static int insert_cost = 1;
	static int delete_cost = 1;

	static int row, col, index = 0; //dynamic programming counters
	static int radix; //radix for modular indexing
	static int low;
	static int[] buffer; //pointer to storage for one row of the d.p. array

	static int[] store = new int[THRESHOLD / (Integer.SIZE / 8)];

	// a small amount of static storage, to be used when the input strings are small enough
	// Handle trivial cases when one string is empty

	static int ins = 1;
	static int del = 1;
	static int ch = 3;
	static int swap_cost = 5;

	static int from_len;
	static int to_len;

	private static int ar(int x, int y, int index) {
		return (((x) == 0) ? (y) * del : (((y) == 0) ? (x) * ins
				: buffer[mod(index)]));
	}

	private static int NW(int x, int y) {
		return ar(x, y, index + from_len + 2);
	}

	private static int N(int x, int y) {
		return ar(x, y, index + from_len + 3);
	}

	private static int W(int x, int y) {
		return ar(x, y, index + radix - 1);
	}

	private static int NNWW(int x, int y) {
		return ar(x, y, index + 1);
	}

	private static int mod(int x) {
		return ((x) % radix);
	}

	/**
	 * edit_distn
	 * 
	 * @param from
	 * @param _from_len
	 * @param to
	 * @param _to_len
	 * @return - returns the edit distance between two strings, or -1 on failure
	 */
	public static int edit_distn(byte[] from, int _from_len, byte[] to,
			int _to_len) {
		from_len = _from_len;
		to_len = _to_len;

		if (from == null) {
			if (to == null) {
				return 0;
			} else {
				return to_len * insert_cost;
			}
		} else if (to == null) {
			return from_len * delete_cost;
		}

		// initialize registers
		radix = 2 * from_len + 3;

		 // Make from short enough to fit in the static storage, if it's at all possible
		if (from_len > to_len && from_len > STRLENTHRESHOLD) {
			return edit_distn(to, to_len, from, from_len);
		}

		// allocate the array storage (from the heap if necessary)
		if (from_len <= STRLENTHRESHOLD) {
			buffer = store;
		} else {
			buffer = new int[radix];
		}

		/*
		 * Here's where the fun begins. We will find the minimum edit distance
		 * using dynamic programming. We only need to store two rows of the
		 * matrix at a time, since we always progress down the matrix. For
		 * example, given the strings "one" and "two", and insert, delete and
		 * change costs equal to 1:
		 * 
		 * _ o n e _ 0 1 2 3 t 1 1 2 3 w 2 2 2 3 o 3 2 3 3
		 * 
		 * The dynamic programming recursion is defined as follows:
		 * 
		 * ar(x,0) := x * insert_cost ar(0,y) := y * delete_cost ar(x,y) :=
		 * min(a(x - 1, y - 1) + (from[x] == to[y] ? 0 : change), a(x - 1, y) +
		 * insert_cost, a(x, y - 1) + delete_cost, a(x - 2, y - 2) + (from[x] ==
		 * to[y-1] && from[x-1] == to[y] ? swap_cost : infinity))
		 * 
		 * Since this only looks at most two rows and three columns back, we
		 * need only store the values for the two preceeding rows. In this
		 * implementation, we do not explicitly store the zero column, so only 2
		 * * from_len + 2 words are needed. However, in the implementation of
		 * the swap_cost check, the current matrix value is used as a buffer; we
		 * can't overwrite the earlier value until the swap_cost check has been
		 * performed. So we use 2 * from_len + 3 elements in the buffer.
		 */

		index = 0;

		// /#define ar(x,y,index) (((x) == 0) ? (y) * del : (((y) == 0) ? (x) *
		// ins :
		// \ buffer[mod(index)]))
		// /#define NW(x,y) ar(x, y, index + from_len + 2)
		// /#define N(x,y) ar(x, y, index + from_len + 3)
		// /#define W(x,y) ar(x, y, index + radix - 1)
		// /#define NNWW(x,y) ar(x, y, index + 1)
		// /#define mod(x) ((x) % radix)
		buffer[index++] = min(ins + del, (from[0] == to[0] ? 0 : ch));

		low = buffer[mod(index + radix - 1)];
		for (col = 1; col < from_len; col++) {
			buffer[index] = min3(col * del + ((from[col] == to[0]) ? 0 : ch),
					(col + 1) * del + ins, buffer[index - 1] + del);
			if (buffer[index] < low) {
				low = buffer[index];
			}
			index++;
		}

		//Now handle the rest of the matrix
		for (row = 1; row < to_len; row++) {
			for (col = 0; col < from_len; col++) {
				buffer[index] = min3(NW(row, col)
						+ ((from[col] == to[row]) ? 0 : ch), N(row, col + 1)
						+ ins, W(row + 1, col) + del);

				if (from[col] == to[row - 1] && col > 0
						&& from[col - 1] == to[row]) {
					buffer[index] = min(buffer[index], NNWW(row - 1, col - 1)
							+ swap_cost);
				}

				if (buffer[index] < low || col == 0) {
					low = buffer[index];
				}
				index = mod(index + 1);
			}
			if (low > MIN_DIST) {
				break;
			}
		}

		row = buffer[mod(index + radix - 1)];

		return row;
	}
}
