import random
import carlbot

# Obviously did not take from http://www.zalgotextgenerator.com/unicode :kappa:
flick_up = ['\u030D', '\u030E', '\u0304', '\u0305', '\u033F',
            '\u0311', '\u0306', '\u0310', '\u0352', '\u0357',
            '\u0351', '\u0307', '\u0308', '\u030A', '\u0342',
            '\u0343', '\u0344', '\u034A', '\u034B', '\u034C',
            '\u0303', '\u0302', '\u030C', '\u0350', '\u0300',
            '\u0301', '\u030B', '\u030F', '\u0312', '\u0313',
            '\u0314', '\u033D', '\u0309', '\u0363', '\u0364',
            '\u0365', '\u0366', '\u0367', '\u0368', '\u0369',
            '\u036A', '\u036B', '\u036C', '\u036D', '\u036E',
            '\u036F', '\u033E', '\u035B', '\u0346', '\u031A']

flick_middle = ['\u0315', '\u031B', '\u0340', '\u0341', '\u0358', '\u0321',
                '\u0322', '\u0327', '\u0328', '\u0334', '\u0335', '\u0336',
                '\u034F', '\u035C', '\u035D', '\u035E', '\u035F', '\u0360',
                '\u0362', '\u0338', '\u0337', '\u0361', '\u0489']

flick_down = ['\u0316', '\u0317', '\u0318', '\u0319', '\u031C',
              '\u031D', '\u031E', '\u031F', '\u0320', '\u0324',
              '\u0325', '\u0326', '\u0329', '\u032A', '\u032B',
              '\u032C', '\u032D', '\u032E', '\u032F', '\u0330',
              '\u0331', '\u0332', '\u0333', '\u0339', '\u033A',
              '\u033B', '\u033C', '\u0345', '\u0347', '\u0348',
              '\u0349', '\u034D', '\u034E', '\u0353', '\u0354',
              '\u0355', '\u0356', '\u0359', '\u035A', '\u0323']

zalgo_pos = ("up", "mid", "down")
zalgo_chars = {"up": flick_up, "mid": flick_middle, "down": flick_down}

random.seed()

@staticmethod
def generate_zalgo(input_charset, count):
    # I checked that python strings are immutable
    # and... there is no string-builder class like C# D:
    # So I decided to 'build' strings using a list instead.
    result = list()
    for i in range(count):
        # Generate a random zalgo character
        # then append to the list.
        result.append(random.choice(input_charset))

    return result


async def str_to_zalgo(args, server, channel, message):
    # Remove the command-arg
    input_str = args.pop(0)

    # If the user did not give any message to process.
    if len(args) < 1:
        input_str = "You ought to give me a text to process before doom undo upon you."

    result = list()
    # For each character in the input string.
    for char in input_str:
        # If the character is a whitespace.
        if char in ' ':
            # Simply add it to the result
            # (No zalgo-ing needed)
            result.append(char)
        else:
            # This will decide how many times this character
            # will be flicked up, down and middle.
            zalgo_counts = {"up": 0, "down": 0, "mid": 0}
            # For each position that we can flick.
            for pos in zalgo_pos:
                # Randomly generate how much times we shall
                # flick on this position for this character.
                zalgo_counts[pos] = random.randint(0, 10)

            # Append to result.
            result.append(char)
            # Now we flick and 'merge' to the char, using Unicode's combing characters.
            for pos in zalgo_pos:
                result += (generate_zalgo(zalgo_chars[pos], zalgo_counts[pos]))

    return ''.join(result)


carlbot.add_command('zalgo', help)
